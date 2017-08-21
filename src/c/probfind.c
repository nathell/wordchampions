#include <stdio.h>
#include <inttypes.h>
#include <stdlib.h>
#include "hashset.h"

const char pl_letters[] = "ąćęłńóśźż";
const int NUM_LETTERS = 32;
const int ASCII_LETTERS = 23;

uint64_t encode(const char *s) {
    uint64_t res = 0;
    while (*s && *s != '\n') {
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
    }
    return res;
}

#define N 20

const char *decode(uint64_t num, size_t sz) {
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

int main() {
    FILE *f;
    char s[20];
    uint64_t w3[3000];
    hashset_t h3 = hashset_create();
    hashset_t h9 = hashset_create();
    size_t i = 0, n = 0, j, k, a = 0;

    f = fopen("w9.txt", "r");
    while (!feof(f)) {
        fgets(s, 20, f);
        hashset_add(h9, (void *)encode(s));
    }
    fclose(f);
    f = fopen("w3.txt", "r");
    while (!feof(f)) {
        fgets(s, 20, f);
        hashset_add(h3, (void *)encode(s));
        w3[n++] = encode(s);
    }
    fclose(f);
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
                    printf("%s\n", decode(wh3, 3));
                }
                a++;
            }
        }
    }
    printf("%ld %d\n", n, hashset_is_member(h9, (void *)encode("aaaabjkbł")));
}
