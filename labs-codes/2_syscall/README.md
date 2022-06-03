# Syscall 实验笔记

## 实验概述

[实验文档](https://pdos.csail.mit.edu/6.S081/2020/labs/syscall.html)

## trace

但是这里缺少了关键的需要阅读文件，如果不看就不懂得做这个实验：

> kernel/kalloc.c

在这个实验里，我们需要让内核输出每个mask变量指定的系统函数的调用情况，格式为：

```text
<pid>: syscall <syscall_name> -> <return_value>
```

pid是进程序号， syscall*name是函数名称，return*value是该系统调用返回值，并且要求各个进程的输出是独立的，不相互干扰，所以我们要在 `kernel/proc.h` 文件的proc结构体中加入一个新的变量，让每个进程都有一个自己的mask。

```c
struct proc {
  struct spinlock lock;

  // p->lock must be held when using these:
  enum procstate state;        // Process state
  struct proc *parent;         // Parent process
  void *chan;                  // If non-zero, sleeping on chan
  int killed;                  // If non-zero, have been killed
  int xstate;                  // Exit status to be returned to parent's wait
  int pid;                     // Process ID

  // these are private to the process, so p->lock need not be held.
  uint64 kstack;               // Virtual address of kernel stack
  uint64 sz;                   // Size of process memory (bytes)
  pagetable_t pagetable;       // User page table
  struct trapframe *trapframe; // data page for trampoline.S
  struct context context;      // swtch() here to run process
  struct file *ofile[NOFILE];  // Open files
  struct inode *cwd;           // Current directory
  char name[16];               // Process name (debugging)
  int mask;
};
```

主要的实现就是在 `usr/syscall.c` 文件的syscall函数，我们先看看函数原型：

```c
void
syscall(void)
{
  int num;
  struct proc *p = myproc();

  num = p->trapframe->a7;//读取系统调用号，等下会说这里怎么来的
  if(num > 0 && num < NELEM(syscalls) && syscalls[num]) {
    p->trapframe->a0 = syscalls[num]();
  } else {
    printf("%d %s: unknown sys call %d\n",
            p->pid, p->name, num);
    p->trapframe->a0 = -1;
  }
}
```

可以看到

```c
p->trapframe->a0 = syscalls[num]();
```

这一行就是调用了系统调用命令，并且把返回值保存在了a0寄存器中（RISCV的C规范是把返回值放在a0中)，所以我们只要在调用系统调用时判断是不是mask规定的输出函数，如果是就输出。

这里有几个点，一个是mask是按位判断的，第二个是proc结构体里的name是整个线程的名字，不是函数调用的函数名称，所以我们不能用p->name，而要自己定义一个数组：

```c
char *sysname[] = {
[SYS_fork]    "fork",
[SYS_exit]    "exit",
[SYS_wait]    "wait",
[SYS_pipe]    "pipe",
[SYS_read]    "read",
[SYS_kill]    "kill",
[SYS_exec]    "exec",
[SYS_fstat]   "stat",
[SYS_chdir]   "chdir",
[SYS_dup]     "dup",
[SYS_getpid]  "getpid",
[SYS_sbrk]    "sbrk",
[SYS_sleep]   "sleep",
[SYS_uptime]  "uptime",
[SYS_open]    "open",
[SYS_write]   "write",
[SYS_mknod]   "mknod",
[SYS_unlink]  "unlink",
[SYS_link]    "link",
[SYS_mkdir]   "mkdir",
[SYS_close]   "close",
[SYS_trace]   "trace",
};
```

所以syscall实现也很简单如下，只加了两行：

```c
void
syscall(void)
{
  int num;
  struct proc *p = myproc();

  num = p->trapframe->a7;
  if(num > 0 && num < NELEM(syscalls) && syscalls[num]) {
    p->trapframe->a0 = syscalls[num]();
    if((1 << num) & p->mask) {
      printf("%d: syscall %s -> %d\n", p->pid, sysname[num], p->trapframe->a0);
    }
  } else {
    printf("%d %s: unknown sys call %d\n",
            p->pid, p->name, num);
    p->trapframe->a0 = -1;
  }
}
```

那我们怎么把mask这个参数传进来呢，这里官方已经给了trace的用户态函数了：

```c
int
main(int argc, char *argv[])
{
  int i;
  char *nargv[MAXARG];

  if(argc < 3 || (argv[1][0] < '0' || argv[1][0] > '9')){
    fprintf(2, "Usage: %s mask command\n", argv[0]);
    exit(1);
  }

  if (trace(atoi(argv[1])) < 0) {
    fprintf(2, "%s: trace failed\n", argv[0]);
    exit(1);
  }
  
  for(i = 2; i < argc && i < MAXARG; i++){
    nargv[i-2] = argv[i];
  }
  exec(nargv[0], nargv);
  exit(0);
}
```

可以看到 trace 函数传入的是一个数字，那我们只要在系统调用trace里把这个数字给到现在的线程就好了，不过得首先把trace这个系统调用加入到内核中声明，首先是 `/usre/user.h`文件加入，这里声明了用户态可以调用的系统调用

```c
int trace(int);
```

`/user/usys.pl` 文件加入

```text
entry("trace");
```

这里perl语言自动生成汇编语言usys.S，是用户态系统调用接口，可以看到首先把系统调用号压入a7寄存器，然后就直接ecall进入系统内核。而我们刚才syscall那个函数就把a7寄存器的数字读出来调用对应的函数，所以这里就是系统调用用户态和内核态的切换接口。

```text
#usys.S
.global trace
trace:
 li a7, SYS_trace
 ecall
 ret
```

接下来还要给内核态的系统调用trace加上声明和定义，在kernel/syscall.c加上：

```c
extern uint64 sys_sysinfo(void);
```

在下面的函数指针数组*syscalls[]加上：

```text
[SYS_trace]   sys_trace,
```

加上这两个后，syscall函数才能正确解析我们写的系统调用，然后在kerlnel/sysproc.c加上sys_trace的定义实现，只要把传进来的参数给到现有进程的mask就好了：

```c
uint64
sys_trace(void)
{
  int mask;
  if(argint(0, &mask) < 0)
    return -1;
  
  myproc()->mask = mask;
  return 0;
}
```

这里argint这个函数调用了argraw函数，这个函数会去读寄存器a0到a5，所以应该是RISCV习惯是把C语言函数调用参数压入这几个寄存器吧，所以这里的a0就对应我们的mask（这个还不清楚，只是看代码是这样：

```c
static uint64
argraw(int n)
{
  struct proc *p = myproc();
  switch (n) {
  case 0:
    return p->trapframe->a0;
  case 1:
    return p->trapframe->a1;
  case 2:
    return p->trapframe->a2;
  case 3:
    return p->trapframe->a3;
  case 4:
    return p->trapframe->a4;
  case 5:
    return p->trapframe->a5;
  }
  panic("argraw");
  return -1;
}
```

最后记得把Makefile加上：

```text
$U/_trace\
```

就可以运行了。

## sysinfo

#### 第一步，添加 Makefile

根据实验指导书，首先需要将 `$U/_sysinfotest` 添加到 Makefile 的 UPROGS 字段中。

#### 第二步，添加声明

同样需要添加一些声明才能进行编译，启动 qemu。需要以下几步：

1. 在 user/user.h 文件中加入函数声明：`int sysinfo(struct sysinfo*);`，同时添加结构体声明 `struct sysinfo;`；
2. 在 `user/usys.pl` 添加进入内核态的入口函数的声明：`entry("sysinfo");`；
3. 同时在 kernel/syscall.h 中添加系统调用的指令码。

#### 第三步，获取内存信息

可以在 kernel/sysinfo.h 中查看结构体 `struct sysinfo`， 其中只有两个字段，一个是保存空闲内存信息，一个是保存正在运行的进程数目。

两个字段的信息都需要自己写函数调用来获取，先来获取内存信息。内存信息的处理都写在 kernel/kalloc.c 文件中了，内存信息以链表的形式存储，每个节点存储一个物理内存页。

从 kfree 函数中可以发现，每次创建一个页时，将其内容初始化为1，然后将它的下一个节点指向当前节点的 freelist，更新 freelist 为这个新创建的页。也就是说，freelist 指向最后一个可以使用的内存页，它的 next 指向上一个可用的内存页。

因此，我们可以通过遍历所有的 freelist 来获取可用内存页数，然后乘上页大小即可。添加获取内存中剩余空闲内存的函数：

```c
uint64 free_mem(void) 
{
  struct run *r = kmem.freelist;
  uint64 n = 0;
  while (r) {
    n++;
    r = r->next;
  }
  return n * PGSIZE;
}
```

#### 第四步，获取进程数目

所有的进程有关的操作都保存在 /kernel/proc.c 文件中，其中的 proc 数组保存了所有进程。进程有五种状态，我们只需要遍历 proc 数组，计算不为 UNUSED 状态的进程数目即可，函数为：

```c
int n_proc(void)
{
  struct proc *p;
  int n = 0;
  for (p = proc; p < &proc[NPROC]; p++) {
    if (p->state != UNUSED)
      n++;
  }
  return n;
}
```

#### 第五步，声明和调用

在 kernel/defs.h 中添加上面这两个函数的声明：

```c
uint64             free_mem(void);
int             n_proc(void);
```

然后在 kernel/sysproc.c 中的 sys_sysinfo 函数进行调用：

```c
uint64 sys_sysinfo(void)
{
  struct sysinfo info;
  uint64 addr;
  struct proc* p = myproc();
  if(argaddr(0, &addr) < 0) {
    return -1;
  }
  info.freemem = free_mem();
  info.nproc = n_proc();
  if (copyout(p->pagetable, addr, (char*)&info, sizeof(info)) < 0) {
    return -1;
  }
  return 0;
}
```

需要注意的是，这里使用 copyout 方法将内核空间中，相应的地址内容复制到用户空间中。这里就是将 info 的内容复制到进程的虚拟地址内，具体是哪个虚拟地址，由函数传入的参数决定（addr 读取第一个参数并转成地址的形式）。



## 参考博客

[MIT-6.S081-2020实验（xv6-riscv64）二：syscall](https://link.zhihu.com/?target=https%3A//www.cnblogs.com/YuanZiming/p/14218997.html)

[第4章 陷阱和系统调用](https://www.jianshu.com/p/e3416ff6ccf0)

https://zhuanlan.zhihu.com/p/432178901

## 实验心得：

risc-v 汇编指令