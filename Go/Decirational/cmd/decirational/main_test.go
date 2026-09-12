package main

import (
	"strconv"
	"strings"
	"testing"

	dec "github.com/zhmgczh/Decirational/Go/Decirational"
)

// Regression test: bufio.Scanner's default Buffer caps a single line at
// bufio.MaxScanTokenSize (64 KiB, i.e. 65536 bytes). run() used to read
// stdin with such a Scanner and never checked Scanner.Err(), so a line
// longer than that limit made Scan() return false exactly as if the input
// had ended: the line produced no output and the process exited 0, with no
// error printed anywhere. run() now reads with bufio.Reader.ReadString,
// which grows its buffer to fit whatever it reads instead of enforcing a
// fixed maximum.
func TestRunHandlesLineLongerThan64KiB(t *testing.T) {
	const terms = 40000 // "0" followed by 40000 "+1"s is 80001 bytes, safely over 65536.
	expression := "0" + strings.Repeat("+1", terms)
	if len(expression) <= 65536 {
		t.Fatalf("test expression is only %d bytes, need more than 65536 to reproduce the bug", len(expression))
	}

	in := strings.NewReader(expression + "\n")
	var out strings.Builder
	if err := run(dec.NewDecimalIntegerFromInt32, dec.ParseDecimalInteger, "default", 0, in, &out); err != nil {
		t.Fatalf("run returned an error: %v", err)
	}

	got := strings.TrimSpace(out.String())
	want := strconv.Itoa(terms)
	if got != want {
		t.Errorf("long line evaluated to %q, want %q (output was silently empty before the fix)", got, want)
	}
}

// The old Scanner-based loop treated a too-long line as if the input had
// ended, so nothing after it was ever processed either. A long line must
// not swallow the lines that follow it.
func TestRunContinuesAfterLongLine(t *testing.T) {
	const terms = 40000
	longExpression := "0" + strings.Repeat("+1", terms)

	in := strings.NewReader(longExpression + "\n1+1\n")
	var out strings.Builder
	if err := run(dec.NewDecimalIntegerFromInt32, dec.ParseDecimalInteger, "default", 0, in, &out); err != nil {
		t.Fatalf("run returned an error: %v", err)
	}

	got := strings.TrimRight(out.String(), "\n")
	lines := strings.Split(got, "\n")
	if len(lines) != 2 {
		t.Fatalf("got %d output line(s) %q, want 2 (the line after the long one was dropped)", len(lines), lines)
	}
	if want := strconv.Itoa(terms); lines[0] != want {
		t.Errorf("first line = %q, want %q", lines[0], want)
	}
	if lines[1] != "2" {
		t.Errorf("second line = %q, want %q", lines[1], "2")
	}
}

// A final line with no trailing newline (the last line of input at EOF)
// must still be evaluated, matching the pre-existing Scanner-based
// behavior (bufio.ScanLines also returns a final unterminated line).
func TestRunHandlesFinalLineWithoutTrailingNewline(t *testing.T) {
	in := strings.NewReader("1+2")
	var out strings.Builder
	if err := run(dec.NewDecimalIntegerFromInt32, dec.ParseDecimalInteger, "default", 0, in, &out); err != nil {
		t.Fatalf("run returned an error: %v", err)
	}
	if got := strings.TrimSpace(out.String()); got != "3" {
		t.Errorf("got %q, want %q", got, "3")
	}
}
