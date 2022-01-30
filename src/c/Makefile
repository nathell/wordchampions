all: generate

generate: generate.c hashset.c hashset.h
	$(CC) $(CFLAGS) $(LDFLAGS) generate.c hashset.c -o $@

clean:
	rm generate generate.o
