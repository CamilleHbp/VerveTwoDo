// VerveTwoDo's browser gateway owns an independent native sync replica.
// Only the fixed upstream workspace is exposed; private provisioning never reaches browsers.
package main

import (
	"bytes"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/CamilleHbp/VerveTwoDo/sync-engine/mobile"
)

type engine interface{ Call(string, string) string }
type gateway struct {
	engine                                            engine
	origin, legacyOrigin, upstream, directory, assets string
	secure                                            bool
	mu                                                sync.Mutex
	call                                              sync.Mutex
	sessions                                          map[string]int64
	attempts                                          []time.Time
	watchers                                          map[chan struct{}]struct{}
	lastView                                          string
	push                                              pushStore
	pushSender                                        interface {
		Do(*http.Request) (*http.Response, error)
	}
}

func hash(s string) string { h := sha256.Sum256([]byte(s)); return hex.EncodeToString(h[:]) }
func send(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "no-store")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(body)
}
func fail(w http.ResponseWriter, status int) {
	send(w, status, map[string]string{"error": "request_failed"})
}
func (g *gateway) saveSessions() error {
	b, err := json.Marshal(g.sessions)
	if err != nil {
		return err
	}
	temp := filepath.Join(g.directory, "sessions.tmp")
	if err = os.WriteFile(temp, b, 0600); err != nil {
		return err
	}
	f, err := os.OpenFile(temp, os.O_WRONLY, 0600)
	if err != nil {
		return err
	}
	err = f.Sync()
	_ = f.Close()
	if err != nil {
		return err
	}
	return os.Rename(temp, filepath.Join(g.directory, "sessions.json"))
}
func (g *gateway) authenticated(r *http.Request) bool {
	cookie, err := r.Cookie("vervetwodo_session")
	if err != nil {
		return false
	}
	g.mu.Lock()
	defer g.mu.Unlock()
	return g.sessions[hash(cookie.Value)] > time.Now().Unix()
}
func (g *gateway) cookie(w http.ResponseWriter, token string, age int) {
	http.SetCookie(w, &http.Cookie{Name: "vervetwodo_session", Value: token, Path: "/api/", HttpOnly: true, Secure: g.secure, SameSite: http.SameSiteStrictMode, MaxAge: age})
}
func decode(w http.ResponseWriter, r *http.Request, out any) error {
	r.Body = http.MaxBytesReader(w, r.Body, 210000)
	decoder := json.NewDecoder(r.Body)
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(out); err != nil {
		return err
	}
	var extra any
	if err := decoder.Decode(&extra); err != io.EOF {
		return errors.New("extra input")
	}
	return nil
}
func (g *gateway) allowedOrigin(origin string) bool {
	return origin != "" && (origin == g.origin || origin == g.legacyOrigin)
}

func (g *gateway) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Referrer-Policy", "no-referrer")
	w.Header().Set("X-Frame-Options", "DENY")
	w.Header().Set("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
	w.Header().Set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; font-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'")
	if r.URL.Path == "/healthz" {
		send(w, 200, map[string]string{"status": "ok"})
		return
	}
	// Native clients such as the desktop widgets accept the web address the user knows and
	// read the synchronization server from here; the browser API above stays browser-only.
	if r.URL.Path == "/.well-known/vervetwodo-sync" {
		if r.Method != "GET" && r.Method != "HEAD" {
			fail(w, 405)
			return
		}
		send(w, 200, map[string]string{"syncOrigin": g.upstream})
		return
	}
	if !strings.HasPrefix(r.URL.Path, "/api/") {
		g.static(w, r)
		return
	}
	w.Header().Set("Cache-Control", "no-store")
	if r.Method == "GET" && r.URL.Path == "/api/events" {
		if r.Header.Get("Origin") != "" && !g.allowedOrigin(r.Header.Get("Origin")) {
			fail(w, 403)
			return
		}
		if !g.authenticated(r) {
			fail(w, 401)
			return
		}
		g.events(w, r)
		return
	}
	if r.Method != "POST" {
		fail(w, 405)
		return
	}
	// Browser mutations require an exact configured origin, never a forwarded host.
	if !g.allowedOrigin(r.Header.Get("Origin")) || !strings.HasPrefix(r.Header.Get("Content-Type"), "application/json") {
		fail(w, 403)
		return
	}
	if r.URL.Path == "/api/login" {
		g.login(w, r)
		return
	}
	if !g.authenticated(r) {
		fail(w, 401)
		return
	}
	if strings.HasPrefix(r.URL.Path, "/api/push/") {
		g.pushRequest(w, r)
		return
	}
	if r.URL.Path == "/api/logout" {
		g.mu.Lock()
		c, _ := r.Cookie("vervetwodo_session")
		delete(g.sessions, hash(c.Value))
		err := g.saveSessions()
		g.mu.Unlock()
		if err != nil {
			fail(w, 503)
			return
		}
		g.cookie(w, "", -1)
		send(w, 200, map[string]bool{"ok": true})
		return
	}
	method := strings.TrimPrefix(r.URL.Path, "/api/")
	if method != "apply" && method != "check" && method != "state" {
		fail(w, 404)
		return
	}
	var payload json.RawMessage
	if decode(w, r, &payload) != nil {
		fail(w, 400)
		return
	}
	if method == "apply" {
		var request struct {
			Operations []json.RawMessage `json:"operations"`
		}
		if json.Unmarshal(payload, &request) != nil || len(request.Operations) > 200 || len(request.Operations) == 0 {
			fail(w, 400)
			return
		}
	}
	g.call.Lock()
	result := g.engine.Call(method, string(payload))
	g.call.Unlock()
	var response struct {
		Error string `json:"error"`
	}
	if json.Unmarshal([]byte(result), &response) != nil || response.Error != "" {
		fail(w, 503)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	_, _ = io.WriteString(w, result)
}
func (g *gateway) login(w http.ResponseWriter, r *http.Request) {
	var input struct {
		Code string `json:"code"`
	}
	if decode(w, r, &input) != nil || len(input.Code) > 200 {
		fail(w, 400)
		return
	}
	g.mu.Lock()
	now := time.Now()
	recent := g.attempts[:0]
	for _, at := range g.attempts {
		if now.Sub(at) < time.Minute {
			recent = append(recent, at)
		}
	}
	g.attempts = recent
	if len(recent) >= 10 {
		g.mu.Unlock()
		fail(w, 429)
		return
	}
	g.attempts = append(g.attempts, now)
	g.mu.Unlock()
	// Every browser login is checked by the existing owner authentication endpoint.
	body, _ := json.Marshal(input)
	request, err := http.NewRequestWithContext(r.Context(), "POST", g.upstream+"/api/v1/login", bytes.NewReader(body))
	if err != nil {
		fail(w, 503)
		return
	}
	request.Header.Set("Content-Type", "application/json")
	request.Header.Set("X-Sync-Namespace", "studio.camille.vervetwodo.task.v1")
	client := &http.Client{Timeout: 20 * time.Second, CheckRedirect: func(_ *http.Request, _ []*http.Request) error { return http.ErrUseLastResponse }}
	response, err := client.Do(request)
	if err != nil {
		fail(w, 503)
		return
	}
	defer response.Body.Close()
	if response.StatusCode != 200 {
		fail(w, 401)
		return
	}
	// Revoke the temporary verification session. The gateway keeps its own engine session.
	var verified struct {
		Token string `json:"token"`
	}
	if json.NewDecoder(io.LimitReader(response.Body, 4<<20)).Decode(&verified) != nil || verified.Token == "" {
		fail(w, 503)
		return
	}
	defer func() {
		req, _ := http.NewRequest("POST", g.upstream+"/api/v1/logout", strings.NewReader("{}"))
		req.Header.Set("Content-Type", "application/json")
		req.Header.Set("X-Sync-Namespace", "studio.camille.vervetwodo.task.v1")
		req.Header.Set("Authorization", "Bearer "+verified.Token)
		resp, err := client.Do(req)
		if err == nil {
			resp.Body.Close()
		}
	}()
	g.call.Lock()
	var state struct {
		State struct {
			Authenticated bool `json:"authenticated"`
		} `json:"state"`
		Error string `json:"error"`
	}
	_ = json.Unmarshal([]byte(g.engine.Call("state", "{}")), &state)
	{
		p, _ := json.Marshal(map[string]string{"origin": g.upstream, "code": input.Code})
		_ = json.Unmarshal([]byte(g.engine.Call("connect", string(p))), &state)
	}
	g.call.Unlock()
	if state.Error != "" || !state.State.Authenticated {
		fail(w, 503)
		return
	}
	raw := make([]byte, 32)
	if _, err = rand.Read(raw); err != nil {
		fail(w, 503)
		return
	}
	token := hex.EncodeToString(raw)
	g.mu.Lock()
	for id, expires := range g.sessions {
		if expires <= now.Unix() {
			delete(g.sessions, id)
		}
	}
	if len(g.sessions) >= 100 {
		g.mu.Unlock()
		fail(w, 429)
		return
	}
	if prior, err := r.Cookie("vervetwodo_session"); err == nil {
		delete(g.sessions, hash(prior.Value))
	}
	g.sessions[hash(token)] = now.Add(30 * 24 * time.Hour).Unix()
	err = g.saveSessions()
	if err != nil {
		delete(g.sessions, hash(token))
	}
	g.mu.Unlock()
	if err != nil {
		fail(w, 503)
		return
	}
	g.cookie(w, token, 30*24*3600)
	send(w, 200, map[string]bool{"ok": true})
}
func (g *gateway) static(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" && r.Method != "HEAD" {
		fail(w, 405)
		return
	}
	name := strings.TrimPrefix(r.URL.Path, "/")
	if name == "" {
		name = "index.html"
	}
	if strings.Contains(name, "..") || strings.HasPrefix(name, ".") {
		http.NotFound(w, r)
		return
	}
	path := filepath.Join(g.assets, filepath.FromSlash(name))
	info, err := os.Stat(path)
	if err != nil || info.IsDir() {
		http.NotFound(w, r)
		return
	}
	w.Header().Set("Cache-Control", "no-cache")
	if strings.HasPrefix(name, "assets/") {
		w.Header().Set("Cache-Control", "public, max-age=31536000, immutable")
	}
	file, err := os.Open(path)
	if err != nil {
		http.NotFound(w, r)
		return
	}
	defer file.Close()
	http.ServeContent(w, r, info.Name(), info.ModTime(), file)
}
func env(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
func main() {
	directory := env("DATA_DIR", "./data")
	if err := os.MkdirAll(directory, 0700); err != nil {
		log.Fatal(err)
	}
	e, err := mobile.NewClient(filepath.Join(directory, "replica"))
	if err != nil {
		log.Fatal(err)
	}
	defer e.Close()
	g := &gateway{engine: e, origin: env("PUBLIC_ORIGIN", "http://127.0.0.1:8791"), legacyOrigin: os.Getenv("LEGACY_ORIGIN"), upstream: env("SYNC_ORIGIN", "https://vervetwodo.195-15-214-30.sslip.io"), directory: directory, assets: env("ASSET_DIR", "../dist"), sessions: map[string]int64{}}
	g.secure = strings.HasPrefix(g.origin, "https://")

	if b, err := os.ReadFile(filepath.Join(directory, "sessions.json")); err == nil {
		if json.Unmarshal(b, &g.sessions) != nil {
			log.Fatal("invalid session store")
		}
	} else if !os.IsNotExist(err) {
		log.Fatal(err)
	}
	if err := g.openPush(); err != nil {
		log.Fatal(err)
	}
	e.StartUpdates(updateListener{g.signal})
	go g.reminderLoop()
	server := http.Server{Addr: env("LISTEN_ADDR", "127.0.0.1:8791"), Handler: g, ReadHeaderTimeout: 5 * time.Second, ReadTimeout: 15 * time.Second, WriteTimeout: 90 * time.Second, IdleTimeout: 60 * time.Second, MaxHeaderBytes: 16384}
	log.Printf("VerveTwoDo web listening on %s", server.Addr)
	log.Fatal(server.ListenAndServe())
}

// Only material record changes notify browsers, avoiding check/event feedback loops.
type updateListener struct{ notify func() }

func (l updateListener) OnChange() { l.notify() }
func (g *gateway) signal() {
	g.call.Lock()
	raw := g.engine.Call("state", "{}")
	g.call.Unlock()
	var result struct {
		State struct {
			Records json.RawMessage `json:"records"`
		} `json:"state"`
	}
	if json.Unmarshal([]byte(raw), &result) != nil {
		return
	}
	digest := hash(string(result.State.Records))
	g.mu.Lock()
	defer g.mu.Unlock()
	if digest == g.lastView {
		return
	}
	g.lastView = digest
	for ch := range g.watchers {
		select {
		case ch <- struct{}{}:
		default:
		}
	}
}
func (g *gateway) events(w http.ResponseWriter, r *http.Request) {
	flusher, ok := w.(http.Flusher)
	if !ok {
		fail(w, 500)
		return
	}
	changes := make(chan struct{}, 1)
	g.mu.Lock()
	if g.watchers == nil {
		g.watchers = map[chan struct{}]struct{}{}
	}
	g.watchers[changes] = struct{}{}
	g.mu.Unlock()
	defer func() { g.mu.Lock(); delete(g.watchers, changes); g.mu.Unlock() }()
	w.Header().Set("Content-Type", "text/event-stream")
	w.Header().Set("X-Accel-Buffering", "no")
	_, _ = io.WriteString(w, "retry: 3000\nevent: ready\ndata: {}\n\n")
	flusher.Flush()
	end := time.NewTimer(60 * time.Second)
	defer end.Stop()
	for {
		select {
		case <-r.Context().Done():
			return
		case <-end.C:
			return
		case <-changes:
			if !g.authenticated(r) {
				return
			}
			_, _ = io.WriteString(w, "data: {}\n\n")
			flusher.Flush()
		}
	}
}
