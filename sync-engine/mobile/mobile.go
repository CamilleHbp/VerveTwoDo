// Package mobile exposes the same pinned sync-core client to gomobile and Go hosts.
package mobile

import (
	"context"
	"encoding/json"
	"github.com/CamilleHbp/VerveTwoDo/sync-engine/tasks"
	core "github.com/CamilleHbp/sync-core"
	"github.com/CamilleHbp/sync-core/appclient"
	"log"
	"sync"
	"time"
	_ "time/tzdata"
)

type UpdateListener interface{ OnChange() }
type Client struct {
	inner *appclient.Client
	mu    sync.Mutex
	stop  context.CancelFunc
}

func (c *Client) StartUpdates(listener UpdateListener) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.stop != nil {
		c.stop()
	}
	ctx, cancel := context.WithCancel(context.Background())
	c.stop = cancel
	go func() {
		for {
			select {
			case <-ctx.Done():
				return
			case <-c.inner.Done():
				return
			case <-c.inner.Changes():
				if listener != nil {
					listener.OnChange()
				}
			}
		}
	}()
}
func (c *Client) StopUpdates() {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.stop != nil {
		c.stop()
		c.stop = nil
	}
}

func NewClient(directory string) (*Client, error) {
	client, err := appclient.New(directory, tasks.Config())
	if err != nil {
		return nil, err
	}
	return &Client{inner: client}, nil
}
func (c *Client) Close() error { c.StopUpdates(); return c.inner.Close() }
func (c *Client) Call(method, payload string) string {
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()
	var err error
	switch method {
	case "state":
	case "connect":
		var p struct {
			Origin string `json:"origin"`
			Code   string `json:"code"`
		}
		if err = json.Unmarshal([]byte(payload), &p); err == nil {
			err = c.inner.Connect(ctx, p.Origin, p.Code)
		}
	case "apply":
		var p struct {
			Operations []core.Operation `json:"operations"`
		}
		if err = json.Unmarshal([]byte(payload), &p); err == nil {
			err = c.inner.Apply(ctx, p.Operations)
		}
	case "sync":
		err = c.inner.Sync(ctx)
	case "check":
		err = c.inner.Check(ctx)
	default:
		err = core.NewError("invalid_request", nil)
	}
	view, viewErr := c.inner.View()
	if err == nil {
		err = viewErr
	}
	if err != nil {
		log.Printf("sync %s failed: %v", method, err)
	}
	data, marshalErr := json.Marshal(struct {
		State     core.ClientView `json:"state"`
		Error     string          `json:"error,omitempty"`
		Telemetry core.SyncStatus `json:"telemetry"`
	}{view, core.ErrorCode(err), view.SyncStatus})
	if marshalErr != nil {
		return `{"error":"sync_failed"}`
	}
	return string(data)
}
