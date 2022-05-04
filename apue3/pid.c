#include "apue.h"

int main(void) {
    printf("Hello world from process ID %ld\n", (long)getpid());
    exit(0);
}