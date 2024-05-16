package main

import (
	"bytes"
	"io"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestIsEarthlyAmazing(t *testing.T) {
	orgStdOut := os.Stdout

	r, w, err := os.Pipe()
	assert.NoError(t, err)

	os.Stdout = w

	main()
	w.Close()

	buf := bytes.Buffer{}

	io.Copy(&buf, r)
	r.Close()

	assert.Equal(t, "Hello World\n", buf.String())
	os.Stdout = orgStdOut
}
