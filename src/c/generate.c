#include <stdio.h>
#include <inttypes.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include "hashset.h"

const char pl_letters[] = "ąćęłńóśźż";
const int NUM_LETTERS = 32;
const int ASCII_LETTERS = 23;

uint64_t (*encode)(const char *, size_t);
const char *(*decode)(uint64_t, size_t);

uint64_t encode_pl(const char *s, size_t sz) {
    uint64_t res = 0;
    while (sz) {
        uint64_t x;
        if (*s >= 'a' && *s <= 'z') {
            x = *s - 'a';
            if (*s > 'q') x--;
            if (*s > 'v') x--;
            if (*s > 'x') x--;
            s++;
        } else {
            const uint16_t c = *(const uint16_t *)s;
            const uint16_t *l = (const uint16_t *)pl_letters;
            x = ASCII_LETTERS;
            while (x < NUM_LETTERS && *l != c) l++, x++;
            if (x == NUM_LETTERS) return -1;

            s += 2;
        }
        res = NUM_LETTERS * res + x;
        sz--;
    }
    return res;
}

uint64_t encode_en(const char *s, size_t sz) {
    uint64_t res = 0;
    while (sz) {
        uint64_t x = *s++ - 'a';
        res = NUM_LETTERS * res + x;
        sz--;
    }
    return res;
}

#define N 20

const char *decode_pl(uint64_t num, size_t sz) {
    static char res[N];
    res[N - 1] = 0;
    char *s = res + N - 2;
    while (sz--) {
        int x = num % NUM_LETTERS;
        if (x < ASCII_LETTERS) {
            *s = x + 'a';
            if (x > 15) (*s)++;
            if (x > 19) (*s)++;
            if (x > 20) (*s)++;
            s--;
        } else {
            const char *u = pl_letters + 2 * (x - ASCII_LETTERS);
            *s = *(u + 1);
            *(s - 1) = *u;
            s -= 2;
        }
        num /= NUM_LETTERS;
    }
    return s + 1;
}

const char *decode_en(uint64_t num, size_t sz) {
    static char res[N];
    res[N - 1] = 0;
    char *s = res + N - 2;
    while (sz--) {
        int x = num % NUM_LETTERS;
        *s-- = x + 'a';
        num /= NUM_LETTERS;
    }
    return s + 1;
}

int cmp(const void *x, const void *y) {
    return *((const char *)x) - *((const char *)y);
}

uint64_t sortnum(uint64_t num) {
    static char arr[N];
    char *s = arr;
    size_t n = 0;
    while (num) {
        *s++ = num % NUM_LETTERS;
        num /= NUM_LETTERS;
        n++;
    }
    qsort(arr, n, 1, cmp);
    s = arr;
    while (n--) {
        num = num * NUM_LETTERS + *s++;
    }
    return num;
}

void unextchar(const char **s) {
    unsigned char c = (unsigned char)**s;
    if      ((c & 0x80) == 0)    *s += 1;
    else if ((c & 0xE0) == 0xC0) *s += 2;
    else if ((c & 0xF0) == 0xE0) *s += 3;
    else if ((c & 0xF8) == 0xF0) *s += 4;
}

size_t ustrlen(const char *s, char terminator) {
    size_t len = 0;
    while (*s != terminator) {
        len++;
        unextchar(&s);
    }
    return len;
}

void *slurp(const char *filename, size_t *sizeptr) {
    struct stat statbuf;
    size_t size;
    char *buf;
    size_t done = 0, todo;
    FILE *f;

    if (stat(filename, &statbuf) == -1) {
        perror(filename);
        exit(1);
    }

    size = (size_t)statbuf.st_size;
    todo = size;

    buf = (char *)malloc(size + 1);
    if (buf == NULL) {
        fprintf(stderr, "Out of memory\n");
        exit(1);
    }

    f = fopen(filename, "rb");
    if (f == NULL) {
        perror(filename);
        exit(1);
    }

    while (todo) {
        size_t numread = fread(buf + done, 1, todo, f);
        if (numread == 0) {
            fprintf(stderr, "Error reading %s\n", filename);
            exit(1);
        }
        done += numread;
        todo -= numread;
    }
    buf[size] = 0;

    *sizeptr = size;
    return (void *)buf;
}

void print_base(char *nines, uint64_t w9) {
    char *wordstmp = nines;
    int found = 0;
    while (*wordstmp) {
        char *next = strchr(wordstmp, '\n');
        if (sortnum(encode(wordstmp, 9)) == w9) {
            if (found) putchar(';');
            found = 1;
            char tmp = *next;
            *next = 0;
            printf("%s", wordstmp);
            *next = tmp;
        };
        wordstmp = next;
        if (wordstmp)
            wordstmp++;
    }
}

const char *wputchar(const char *s) {
    const char *tmp = s;
    unextchar(&tmp);
    while (s < tmp) putchar(*s++);
    return tmp;
}

void print_matches(char *words, char *nines, uint64_t w3) {
    char *wordstmp = words;
    int found = 0;
    putchar(',');
    while (wordstmp != nines) {
        char *next = strchr(wordstmp, '\n');
        char *letter = wordstmp;
        unextchar((const char **)&letter);
        uint64_t w = encode(letter, 3);
        if (w == w3) {
            if (found) putchar(';');
            found = 1;
            wputchar(wordstmp);
            unextchar((const char **)&letter);
            unextchar((const char **)&letter);
            unextchar((const char **)&letter);
            wputchar(letter);
        }
        wordstmp = next;
        if (wordstmp)
            wordstmp++;
    }
}

int main(int argc, char **argv) {
    FILE *f;
    char s[20], *words, *wordstmp, *nines;
    uint64_t w3[30000];
    hashset_t h3 = hashset_create();
    hashset_t h9 = hashset_create();
    size_t i = 0, n = 0, j, k, a = 0, filesize, prevlen;
    if (argc < 3) {
        fprintf(stderr, "Usage: %s {pl|en} <file.txt>\n", argv[0]);
        return 1;
    }

    if (strcmp(argv[1], "en") == 0) {
        encode = encode_en;
        decode = decode_en;
    } else {
        encode = encode_pl;
        decode = decode_pl;
    }

    words = (char *)slurp(argv[2], &filesize);

    wordstmp = words;
    prevlen = 0;
    while (*wordstmp) {
        char *next = strchr(wordstmp, '\n');
        size_t len = ustrlen(wordstmp, '\n');
        if (len == 5) {
            uint64_t w;
            unextchar((const char **)&wordstmp);
            w = encode(wordstmp, 3);
            if (!hashset_is_member(h3, (void *)w))
                w3[n++] = w;
            hashset_add(h3, (void *)w);
        } else if (len == 9) {
            if (prevlen == 5)
                nines = wordstmp;
            hashset_add(h9, (void *)sortnum(encode(wordstmp, 9)));
        };
        prevlen = len;
        wordstmp = next;
        if (wordstmp)
            wordstmp++;
    }

    for (i = 0; i < n; i++) {
        uint64_t wh1 = w3[i];
        for (j = 0; j < n; j++) {
            uint64_t wh2 = w3[j];
            for (k = 0; k < n; k++) {
                uint64_t wh3 = w3[k];
                uint64_t
                    wv1 =
                    ((wh1 >> 10) << 10) +
                    ((wh2 >> 10) << 5) +
                    (wh3 >> 10),
                    wv2 =
                    (((wh1 >> 5) & 31) << 10) +
                    (((wh2 >> 5) & 31) << 5) +
                    ((wh3 >> 5) & 31),
                    wv3 =
                    ((wh1 & 31) << 10) +
                    ((wh2 & 31) << 5) +
                    (wh3 & 31),
                    all = sortnum((wh1 << 30) + (wh2 << 15) + wh3);
                if (hashset_is_member(h3, (void *)wv1) &&
                    hashset_is_member(h3, (void *)wv2) &&
                    hashset_is_member(h3, (void *)wv3) &&
                    hashset_is_member(h9, (void *)all))
                {
                    printf("%s", decode(wh1, 3));
                    printf("%s", decode(wh2, 3));
                    printf("%s", decode(wh3, 3));
                    putchar(',');
                    print_base(nines, all);
                    print_matches(words, nines, wh1);
                    print_matches(words, nines, wh2);
                    print_matches(words, nines, wh3);
                    print_matches(words, nines, wv1);
                    print_matches(words, nines, wv2);
                    print_matches(words, nines, wv3);
                    putchar('\n');
                }
                a++;
            }
        }
    }
}
