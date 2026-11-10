package main

import (
	"context"
	_ "embed"
	"encoding/base64"
	"encoding/json"
	"errors"
	core "github.com/CamilleHbp/sync-core"
	webpush "github.com/SherClockHolmes/webpush-go"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

//go:embed push-messages.json
var pushMessagesJSON []byte

func pushBody(locale string) string {
	var messages map[string]string
	_ = json.Unmarshal(pushMessagesJSON, &messages)
	if messages[locale] != "" {
		return messages[locale]
	}
	return messages["en"]
}

type pushConfig struct {
	Subscription webpush.Subscription `json:"subscription"`
	Session      string               `json:"session"`
	Locale       string               `json:"locale"`
	TimeZone     string               `json:"timeZone"`
	DefaultTime  string               `json:"defaultTime"`
	Advance      int                  `json:"advance"`
	EnabledAt    int64                `json:"enabledAt"`
	Sent         map[string]int64     `json:"sent"`
}
type pushStore struct {
	Public  string                `json:"public"`
	Private string                `json:"private"`
	Devices map[string]pushConfig `json:"devices"`
}

func (g *gateway) savePush() error {
	b, err := json.Marshal(g.push)
	if err != nil {
		return err
	}
	tmp := filepath.Join(g.directory, "push.tmp")
	if err = os.WriteFile(tmp, b, 0600); err != nil {
		return err
	}
	return os.Rename(tmp, filepath.Join(g.directory, "push.json"))
}
func (g *gateway) openPush() error {
	b, err := os.ReadFile(filepath.Join(g.directory, "push.json"))
	if err == nil {
		return json.Unmarshal(b, &g.push)
	}
	if !os.IsNotExist(err) {
		return err
	}
	private, public, err := webpush.GenerateVAPIDKeys()
	if err != nil {
		return err
	}
	g.push = pushStore{Public: public, Private: private, Devices: map[string]pushConfig{}}
	return g.savePush()
}
func validSubscription(s webpush.Subscription) bool {
	u, err := url.Parse(s.Endpoint)
	if err != nil || u.Scheme != "https" || u.User != nil || u.Fragment != "" || u.Port() != "" || len(s.Endpoint) > 2048 {
		return false
	}
	host := strings.ToLower(u.Hostname())
	allowed := host == "fcm.googleapis.com"
	for _, suffix := range []string{".push.services.mozilla.com", ".push.apple.com", ".notify.windows.com"} {
		allowed = allowed || strings.HasSuffix(host, suffix)
	}
	auth, e1 := base64.RawURLEncoding.DecodeString(strings.TrimRight(s.Keys.Auth, "="))
	key, e2 := base64.RawURLEncoding.DecodeString(strings.TrimRight(s.Keys.P256dh, "="))
	return allowed && e1 == nil && e2 == nil && len(auth) == 16 && len(key) == 65 && key[0] == 4
}
func (g *gateway) pushRequest(w http.ResponseWriter, r *http.Request) {
	cookie, _ := r.Cookie("vervetwodo_session")
	session := hash(cookie.Value)
	if r.URL.Path == "/api/push/key" {
		g.mu.Lock()
		key := g.push.Public
		g.mu.Unlock()
		send(w, 200, map[string]string{"key": key})
		return
	}
	if r.URL.Path == "/api/push/disable" {
		g.mu.Lock()
		for id, c := range g.push.Devices {
			if c.Session == session {
				delete(g.push.Devices, id)
			}
		}
		err := g.savePush()
		g.mu.Unlock()
		if err != nil {
			fail(w, 503)
			return
		}
		send(w, 200, map[string]bool{"ok": true})
		return
	}
	if r.URL.Path != "/api/push/subscribe" {
		fail(w, 404)
		return
	}
	var input struct {
		Subscription webpush.Subscription `json:"subscription"`
		Locale       string               `json:"locale"`
		TimeZone     string               `json:"timeZone"`
		DefaultTime  string               `json:"defaultTime"`
		Advance      int                  `json:"advance"`
	}
	if decode(w, r, &input) != nil || !validSubscription(input.Subscription) || input.Advance < 0 || input.Advance > 10080 || (input.Locale != "en" && input.Locale != "fr") {
		fail(w, 400)
		return
	}
	if _, err := time.LoadLocation(input.TimeZone); err != nil {
		fail(w, 400)
		return
	}
	if _, err := time.Parse("15:04", input.DefaultTime); err != nil {
		fail(w, 400)
		return
	}
	g.mu.Lock()
	defer g.mu.Unlock()
	id := hash(input.Subscription.Endpoint)
	previous, exists := g.push.Devices[id]
	if !exists && len(g.push.Devices) >= 100 {
		fail(w, 429)
		return
	}
	c := pushConfig{Subscription: input.Subscription, Session: session, Locale: input.Locale, TimeZone: input.TimeZone, DefaultTime: input.DefaultTime, Advance: input.Advance, EnabledAt: time.Now().Unix(), Sent: map[string]int64{}}
	if exists {
		c.EnabledAt = previous.EnabledAt
		c.Sent = previous.Sent
	}
	g.push.Devices[id] = c
	if err := g.savePush(); err != nil {
		if exists {
			g.push.Devices[id] = previous
		} else {
			delete(g.push.Devices, id)
		}
		fail(w, 503)
		return
	}
	send(w, 200, map[string]bool{"ok": true})
}
func reminderTime(r core.Record, c pushConfig) (time.Time, error) {
	if r.Kind != "task" || r.Value("completed") == "true" || r.Value("deleted") == "true" || r.Value("retracted") == "true" || r.Value("dueDate") == "" {
		return time.Time{}, errors.New("not scheduled")
	}
	clock, zone := r.Value("dueTime"), r.Value("timeZone")
	if clock == "" {
		clock = c.DefaultTime
		zone = c.TimeZone
	}
	location, err := time.LoadLocation(zone)
	if err != nil {
		return time.Time{}, err
	}
	return time.ParseInLocation("2006-01-02 15:04", r.Value("dueDate")+" "+clock, location)
}
func (g *gateway) deliverReminders() {
	g.call.Lock()
	raw := g.engine.Call("state", "{}")
	g.call.Unlock()
	var reply struct {
		State struct {
			Records []core.Record `json:"records"`
		} `json:"state"`
	}
	if json.Unmarshal([]byte(raw), &reply) != nil {
		return
	}
	g.mu.Lock()
	configs := map[string]pushConfig{}
	now := time.Now()
	for id, c := range g.push.Devices {
		if g.sessions[c.Session] > now.Unix() {
			copy := c
			copy.Sent = map[string]int64{}
			for k, v := range c.Sent {
				copy.Sent[k] = v
			}
			configs[id] = copy
		}
	}
	public, private := g.push.Public, g.push.Private
	g.mu.Unlock()
	for id, c := range configs {
		for _, record := range reply.State.Records {
			due, err := reminderTime(record, c)
			if err != nil {
				continue
			}
			offsets := []int{0}
			if c.Advance > 0 {
				offsets = append(offsets, c.Advance)
			}
			for _, offset := range offsets {
				at := due.Add(-time.Duration(offset) * time.Minute)
				key := record.ID + ":" + strconv.Itoa(offset)
				if at.After(now) || at.Unix() < c.EnabledAt || now.Sub(at) > time.Hour || c.Sent[key] == at.Unix() {
					continue
				}
				body := pushBody(c.Locale)
				title := []rune(record.Value("title"))
				if len(title) > 200 {
					title = title[:200]
				}
				payload, _ := json.Marshal(map[string]string{"title": string(title), "body": body, "taskId": record.ID, "tag": key})
				g.mu.Lock()
				active, exists := g.push.Devices[id]
				valid := exists && active.Session == c.Session && g.sessions[c.Session] > time.Now().Unix()
				g.mu.Unlock()
				if !valid {
					break
				}
				ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
				var sender webpush.HTTPClient = &http.Client{Timeout: 10 * time.Second, CheckRedirect: func(_ *http.Request, _ []*http.Request) error { return http.ErrUseLastResponse }}
				if g.pushSender != nil {
					sender = g.pushSender
				}
				response, err := webpush.SendNotificationWithContext(ctx, payload, &c.Subscription, &webpush.Options{Subscriber: g.origin, VAPIDPublicKey: public, VAPIDPrivateKey: private, TTL: 300, HTTPClient: sender})
				cancel()
				if err != nil {
					continue
				}
				response.Body.Close()
				g.mu.Lock()
				current, exists := g.push.Devices[id]
				if exists {
					if response.StatusCode == 404 || response.StatusCode == 410 {
						delete(g.push.Devices, id)
					} else if response.StatusCode >= 200 && response.StatusCode < 300 {
						current.Sent[key] = at.Unix()
						for old, stamp := range current.Sent {
							if stamp < now.Add(-30*24*time.Hour).Unix() {
								delete(current.Sent, old)
							}
						}
						g.push.Devices[id] = current
					}
					_ = g.savePush()
				}
				g.mu.Unlock()
			}
		}
	}
}
func (g *gateway) reminderLoop() {
	timer := time.NewTicker(20 * time.Second)
	defer timer.Stop()
	for range timer.C {
		g.deliverReminders()
	}
}
