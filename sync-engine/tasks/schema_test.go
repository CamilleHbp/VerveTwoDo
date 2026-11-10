package tasks

import (
	core "github.com/CamilleHbp/sync-core"
	"testing"
)

func TestCategoryIconOperation(t *testing.T) {
	for _, value := range []string{"tag", "cinema", "future-icon"} {
		if err := validateOperation(core.Operation{Kind: "tag", Field: "icon", Value: value}); err != nil {
			t.Fatal(value, err)
		}
	}
	if err := validateOperation(core.Operation{Kind: "tag", Field: "icon", Value: "../../secret"}); err == nil {
		t.Fatal("path should not be accepted as an icon ID")
	}
}

func TestRolloverOperation(t *testing.T) {
	for _, value := range []string{"true", "false"} {
		if err := validateOperation(core.Operation{Kind: "task", Field: "rollover", Value: value}); err != nil {
			t.Fatal(err)
		}
	}
	if err := validateOperation(core.Operation{Kind: "task", Field: "rollover", Value: "yes"}); err == nil {
		t.Fatal("invalid rollover accepted")
	}
}
