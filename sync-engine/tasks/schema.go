// Package tasks defines VerveTwoDo's application schema, outside sync-core.
package tasks

import (
	"encoding/json"
	core "github.com/CamilleHbp/sync-core"
	"slices"
	"strconv"
	"strings"
	"time"
)

const Namespace = "studio.camille.vervetwodo.task.v1"

func Config() core.Config {
	return core.Config{Namespace: Namespace, Platforms: []string{"android", "macos", "windows", "linux"}, ValidateOperation: validateOperation, ValidateRecords: validateRecords}
}

var taskFields = " title details completed priority dueDate dueTime timeZone checklist recurrence rollover tags parentId deleted retracted createdAt "

func validateOperation(op core.Operation) error {
	invalid := func() error { return core.NewError("invalid_task", nil) }
	if op.Kind == "tag" {
		switch op.Field {
		case "name":
			if strings.TrimSpace(op.Value) == "" || len(op.Value) > 500 {
				return invalid()
			}
		case "icon":
			if len(op.Value) > 64 || strings.ContainsAny(op.Value, "/\\. ") {
				return invalid()
			}
		case "color":
			if len(op.Value) != 7 || op.Value[0] != '#' {
				return invalid()
			}
			if _, err := strconv.ParseUint(op.Value[1:], 16, 24); err != nil {
				return invalid()
			}
		case "deleted":
			if op.Value != "true" && op.Value != "false" {
				return invalid()
			}
		default:
			return invalid()
		}
		return nil
	}
	if op.Kind != "task" || !slices.Contains(strings.Fields(taskFields), op.Field) || op.Field == "" {
		return invalid()
	}
	switch op.Field {
	case "title":
		if len(op.Value) > 16000 || strings.TrimSpace(op.Value) == "" {
			return invalid()
		}
	case "details":
		if len(op.Value) > 100000 {
			return invalid()
		}
	case "completed", "deleted", "retracted", "rollover":
		if op.Value != "true" && op.Value != "false" {
			return invalid()
		}
	case "priority":
		n, err := strconv.Atoi(op.Value)
		if err != nil || n < 0 || n > 4 {
			return invalid()
		}
	case "dueDate":
		if op.Value != "" {
			if _, err := time.Parse("2006-01-02", op.Value); err != nil {
				return invalid()
			}
		}
	case "dueTime":
		if op.Value != "" {
			if _, err := time.Parse("15:04", op.Value); err != nil {
				return invalid()
			}
		}
	case "timeZone":
		if op.Value != "" {
			if _, err := time.LoadLocation(op.Value); err != nil {
				return invalid()
			}
		}
	case "createdAt":
		if op.Value != "" {
			if _, err := strconv.ParseInt(op.Value, 10, 64); err != nil {
				return invalid()
			}
		}
	case "parentId":
		if op.Value != "" && (len(op.Value) < 16 || len(op.Value) > 128) {
			return invalid()
		}
	case "tags":
		var ids []string
		if json.Unmarshal([]byte(op.Value), &ids) != nil || len(ids) > 100 {
			return invalid()
		}
		seen := map[string]bool{}
		for _, id := range ids {
			if len(id) < 16 || len(id) > 128 || seen[id] {
				return invalid()
			}
			seen[id] = true
		}
	case "checklist":
		var items []struct {
			ID        string `json:"id"`
			Title     string `json:"title"`
			Completed bool   `json:"completed"`
		}
		if json.Unmarshal([]byte(op.Value), &items) != nil || len(items) > 500 {
			return invalid()
		}
		seen := map[string]bool{}
		for _, item := range items {
			if item.ID == "" || seen[item.ID] || strings.TrimSpace(item.Title) == "" || len(item.Title) > 16000 {
				return invalid()
			}
			seen[item.ID] = true
		}
	case "recurrence":
		if op.Value != "" && op.Value != "null" {
			var r struct {
				Frequency string `json:"frequency"`
				Interval  int    `json:"interval"`
				Anchor    string `json:"anchor"`
			}
			if json.Unmarshal([]byte(op.Value), &r) != nil || r.Interval < 1 || r.Interval > 999 || !slices.Contains([]string{"daily", "weekly", "monthly", "yearly"}, r.Frequency) || r.Frequency == "" {
				return invalid()
			}
			if _, err := time.Parse("2006-01-02", r.Anchor); err != nil {
				return invalid()
			}
		}
	}
	return nil
}
func validateRecords(view core.View) error {
	for _, record := range view.Records {
		if record.Value("deleted") == "true" {
			continue
		}
		field := "title"
		if record.Kind == "tag" {
			field = "name"
		}
		if record.Value(field) == "" {
			return core.NewError("invalid_task", nil)
		}
	}
	return nil
}
