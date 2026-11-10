package main

import (
	"crypto/elliptic"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	core "github.com/CamilleHbp/sync-core"
	webpush "github.com/SherClockHolmes/webpush-go"
	"io"
	"net/http"
	"strings"
	"testing"
	"time"
)

func subscription(t *testing.T) webpush.Subscription {
	t.Helper()
	_, x, y, err := elliptic.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		t.Fatal(err)
	}
	return webpush.Subscription{Endpoint: "https://fcm.googleapis.com/test", Keys: webpush.Keys{Auth: base64.RawURLEncoding.EncodeToString(make([]byte, 16)), P256dh: base64.RawURLEncoding.EncodeToString(elliptic.Marshal(elliptic.P256(), x, y))}}
}
func TestPushEndpointsAndKeys(t *testing.T) {
	s := subscription(t)
	if !validSubscription(s) {
		t.Fatal("valid provider rejected")
	}
	for _, endpoint := range []string{"http://fcm.googleapis.com/x", "https://127.0.0.1/x", "https://fcm.googleapis.com.evil.test/x", "https://user@fcm.googleapis.com/x", "https://fcm.googleapis.com:444/x"} {
		s.Endpoint = endpoint
		if validSubscription(s) {
			t.Fatal(endpoint)
		}
	}
}
func reminderRecord(date, clock, zone string) core.Record {
	return core.Record{ID: "test-task-12345678", Kind: "task", Fields: map[string][]core.Version{"title": {{ID: "title", Value: "Task"}}, "dueDate": {{ID: "date", Value: date}}, "dueTime": {{ID: "time", Value: clock}}, "timeZone": {{ID: "zone", Value: zone}}}}
}
func TestPushReminderTimeAndCancellation(t *testing.T) {
	c := pushConfig{DefaultTime: "09:00", TimeZone: "Europe/Paris"}
	r := reminderRecord("2026-07-01", "", "")
	at, err := reminderTime(r, c)
	if err != nil || at.UTC().Format(time.RFC3339) != "2026-07-01T07:00:00Z" {
		t.Fatal(at, err)
	}
	r.Fields["completed"] = []core.Version{{Value: "true"}}
	if _, err = reminderTime(r, c); err == nil {
		t.Fatal("completed task scheduled")
	}
	r = reminderRecord("2026-12-01", "10:00", "America/New_York")
	at, err = reminderTime(r, c)
	if err != nil || at.UTC().Format(time.RFC3339) != "2026-12-01T15:00:00Z" {
		t.Fatal(at, err)
	}
}

type notificationEngine struct{ r core.Record }

func (e notificationEngine) Call(_, _ string) string {
	b, _ := json.Marshal(map[string]any{"state": map[string]any{"records": []core.Record{e.r}}})
	return string(b)
}

type capturePush struct {
	calls  int
	status int
}

func (c *capturePush) Do(r *http.Request) (*http.Response, error) {
	c.calls++
	if r.Header.Get("Authorization") == "" {
		panic("missing VAPID")
	}
	return &http.Response{StatusCode: c.status, Body: io.NopCloser(strings.NewReader(""))}, nil
}
func TestPushDeliveryDeduplicatesAndHonorsSessionRevocation(t *testing.T) {
	g := setup(t)
	if err := g.openPush(); err != nil {
		t.Fatal(err)
	}
	now := time.Now().UTC()
	g.engine = notificationEngine{reminderRecord(now.Format("2006-01-02"), now.Format("15:04"), "UTC")}
	sender := &capturePush{status: 201}
	g.pushSender = sender
	s := subscription(t)
	g.push.Devices[hash(s.Endpoint)] = pushConfig{Subscription: s, Session: hash("valid"), Locale: "en", TimeZone: "UTC", DefaultTime: "09:00", EnabledAt: now.Add(-time.Hour).Unix(), Sent: map[string]int64{}}
	g.deliverReminders()
	g.deliverReminders()
	if sender.calls != 1 {
		t.Fatal("duplicate or missing push", sender.calls)
	}
	c := g.push.Devices[hash(s.Endpoint)]
	c.Sent = map[string]int64{}
	g.push.Devices[hash(s.Endpoint)] = c
	delete(g.sessions, hash("valid"))
	g.deliverReminders()
	if sender.calls != 1 {
		t.Fatal("revoked session notified")
	}
}
func TestPushKeysPersistAndSubscriptionAPI(t *testing.T) {
	g := setup(t)
	if err := g.openPush(); err != nil {
		t.Fatal(err)
	}
	key := g.push.Public
	if err := g.openPush(); err != nil || g.push.Public != key {
		t.Fatal("VAPID identity changed")
	}
	body, _ := json.Marshal(map[string]any{"subscription": subscription(t), "locale": "fr", "timeZone": "Europe/Paris", "defaultTime": "09:00", "advance": 15})
	if r := request(g, "POST", "/api/push/subscribe", g.origin, "valid", string(body)); r.Code != 200 {
		t.Fatal(r.Code, r.Body.String())
	}
	if len(g.push.Devices) != 1 {
		t.Fatal("not stored")
	}
	if r := request(g, "POST", "/api/push/disable", g.origin, "valid", "{}"); r.Code != 200 || len(g.push.Devices) != 0 {
		t.Fatal("not disabled")
	}
}
