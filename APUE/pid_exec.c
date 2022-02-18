#include "apue.h"

// 从标准输入读入命令并执行
int main(int argc, char const *argv[]) {
    char buf[MAXLINE];
    pid_t pid;
    int status;

    printf("%% ");
    while (fgets(buf, MAXLINE, stdin) != NULL)
    {
        if (buf[strlen(buf) - 1] == '\n') {
            buf[strlen(buf) - 1] = 0;
        }
        if ((pid = fork()) < 0) {
            err_sys("fork error");
        } else if (pid ==0) {
            execlp(buf, buf, (char *)0);
            err_ret("couldn't execute: %s", buf);
            exit(127);
        }

        /* parent */
        if ((pid == waitpid(pid, &status, 0)) < 0) {
            err_sys("waitpid error");
        }

        printf("%%");
    }
    
    exit(0);
}
