CFLAGS := $(shell pkg-config --cflags glib-2.0)

all: probfind

probfind: probfind.c hashset.c hashset.h
	$(CC) $(CFLAGS) $(LDFLAGS) -o $@ $^

clean:
	rm probfind
