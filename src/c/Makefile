CFLAGS := $(shell pkg-config --cflags glib-2.0)

all: generate

generate: generate.c hashset.c hashset.h
	$(CC) $(CFLAGS) $(LDFLAGS) -o $@ $^

clean:
	rm generate generate.o
