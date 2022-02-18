// urls: https://blog.csdn.net/qq_44111805/article/details/112003500

import java.util.Scanner;

public class Banker {
    String NAME[]       = new String[100];  //资源的名称
    int Max[][]         = new int[100][100];//最大需求矩阵
    int Allocation[][]  = new int[100][100];//系统已分配矩阵
    int Need[][]        = new int[100][100];//还需要资源矩阵
    int Available[]     = new int[100];     //可用资源矩阵
    int Request[]       = new int[100];     //请求资源向量
    int Work[]          = new int[100];     //存放系统可提供资源量
    int Finish[]        = new int[100];     //标记系统是否有足够的资源分配给各个进程
    int Security[]      = new int[100];     //存ß放安全序列


    int Max_Threads  = 100;      //进程的最大数
    int Max_Resource = 100;      //资源的最大数
    Scanner sc = new Scanner(System.in);

    //初始化各类数据
    public void init()
    {
        int i,j,m,n;
        int number;
        boolean flag;
        String name;                        //输入资源名称
        int temp[]=new int[100];            //统计已分配资源

        //输入系统资源数目及各资源初试个数
        System.out.print("系统可用资源种类为:");
        n=sc.nextInt();
        Max_Resource=n;
        for(i=0;i<n;i++)
        {
            sc.nextLine();                  //清空缓冲区否则可能导致无法输入报出异常
            System.out.println("资源"+i+"名称为:");
            name=sc.nextLine();
            NAME[i]=name;
            System.out.println("资源"+name+"初始化个数为:");
            number=sc.nextInt();
            Available[i]=number;
        }

        //输入进程数及各进程的最大需求矩阵
        System.out.println("请输入进程的数量:");
        m=sc.nextInt();
        Max_Threads=m;
        System.out.println("请输入各进程的最大需求矩阵的值[Max]:");
        do {
            flag=false;
            for(i=0;i<Max_Threads;i++)
            {
                for(j=0;j<Max_Resource;j++)
                {
                    Max[i][j]=sc.nextInt();
                    if(Max[i][j]>Available[j])
                    {
                        flag=true;
                    }
                }
            }
            if(flag)
            {
                System.out.println("资源最大需求量大于系统中资源最大量，请重新输入!");
            }
        }while(flag);

        //输入各进程已经分配的资源量，并求得还需要的资源量
        do {
            flag=false;
            System.out.println("请输入各进程已经分配的资源量[Allocation]:");
            for(i=0;i<Max_Threads;i++)
            {
                for(j=0;j<Max_Resource;j++)
                {
                    Allocation[i][j]=sc.nextInt();
                    if(Allocation[i][j]>Max[i][j])
                    {
                        flag=true;
                    }
                    Need[i][j]=Max[i][j]-Allocation[i][j];
                    temp[j]+=Allocation[i][j];//统计已经分配给进程的资源数目
                }
            }
            if(flag)
            {
                System.out.println("分配的资源大于最大量，请重新输入!");
            }
        }while(flag);

        //求得系统中可利用的资源量
        for(j=0;j<Max_Resource;j++)
        {
            Available[j]=Available[j]-temp[j];
        }
    }

    //显示资源分配矩阵
    public void showdata()
    {
        int i,j;
        System.out.println("*************************************************************");
        System.out.println("系统目前可用的资源[Available]:");
        for(i=0;i<Max_Resource;i++)
        {
            System.out.print(NAME[i]+"  ");
        }
        System.out.println();
        for(j=0;j<Max_Resource;j++)
        {
            System.out.print(Available[j]+"  ");
        }
        System.out.println();
        System.out.println("系统当前的资源分配情况如下：");
        System.out.println("         Max   	    Allocation      Need");
        System.out.print("进程名     ");
        //输出与进程名同行的资源名，Max、Allocation、Need下分别对应
        for(j=0;j<3;j++)
        {
            for(i=0;i<Max_Resource;i++)
            {
                System.out.print(NAME[i]+"  ");
            }
            System.out.print("     ");
        }
        System.out.println();
        //输出每个进程的Max、Allocation、Need
        for(i=0;i<Max_Threads;i++)
        {
            System.out.print("P"+i+"    ");
            for(j=0;j<Max_Resource;j++)
            {
                System.out.print(Max[i][j]+"  ");
            }
            System.out.print("     ");
            for(j=0;j<Max_Resource;j++)
            {
                System.out.print(Allocation[i][j]+"  ");
            }
            System.out.print("     ");
            for(j=0;j<Max_Resource;j++)
            {
                System.out.print(Need[i][j]+"  ");
            }
            System.out.println();
        }
    }

    //尝试分配资源
    public int test(int i)
    {
        for(int j=0;j<Max_Resource;j++)
        {
            Available[j]=Available[j]-Request[j];
            Allocation[i][j]=Allocation[i][j]+Request[j];
            Need[i][j]=Need[i][j]-Request[j];
        }
        return 1;
    }

    //试探性分配资源作废，与test操作相反
    public int retest(int i)
    {
        for(int j=0;j<Max_Resource;j++)
        {
            Available[j]=Available[j]+Request[j];
            Allocation[i][j]=Allocation[i][j]-Request[j];
            Need[i][j]=Need[i][j]+Request[j];
        }
        return 1;
    }

    //安全性算法
    public int safe()
    {
        int i,j,k=0,m,apply;
        for(j=0;j<Max_Resource;j++)//初始化work
        {
            Work[j]=Available[j];
        }
        for(i=0;i<Max_Threads;i++)//初始化Finish
        {
            Finish[i]=0;
        }
        for(i=0;i<Max_Threads;i++)
        {
            apply=0;
            for(j=0;j<Max_Resource;j++)
            {
                if(Finish[i]==0&&Need[i][j]<=Work[j])
                {
                    apply++;//直到每类资源尚需数都小于系统可利用资源数才可分配
                    if(apply==Max_Resource)
                    {
                        for(m=0;m<Max_Resource;m++)
                        {
                            Work[m]=Work[m]+Allocation[i][m];//更改当前可分配资源
                        }
                        Finish[i]=1;
                        Security[k++]=i;
                        i=-1; //保证每次查询均从第一个进程开始
                    }
                }
            }
        }
        for(i=0;i<Max_Threads;i++)
        {
            if(Finish[i]==0)
            {
                System.out.println("系统不安全！");
                return 0;
            }
        }
        System.out.println("系统安全！");
        System.out.println("存在一个安全序列：");
        for(i=0;i<Max_Threads;i++)
        {
            System.out.print("P"+Security[i]);
            if(i<Max_Threads-1)
            {
                System.out.print("->");
            }
        }
        System.out.println();
        return 1;
    }

    //银行家算法处理申请资源
    public void bank()
    {
        boolean flag=true;
        int i,j;
        System.out.println("请输入请求分配资源的进程号(0~"+(Max_Threads-1)+"):");
        i=sc.nextInt();
        System.out.println("请输入进程P"+i+"要申请的资源个数:");
        for(j=0;j<Max_Resource;j++)
        {
            System.out.print(NAME[j]+":");
            Request[j]=sc.nextInt();//输入需要申请的资源
        }

        //判断银行家算法的前两条件是否成立
        for(j=0;j<Max_Resource;j++)
        {
            if(Request[j]>Need[i][j])
            {
                System.out.print("进程P"+i+"申请的资源大于系统现在可利用的资源");
                System.out.println("分配不合理，不予分配！");
                flag=false;
                break;
            }
            else
            {
                if(Request[j]>Available[j])
                {
                    System.out.print("进程"+i+"申请的资源大于系统现在可利用的资源");
                    System.out.println();
                    System.out.println("系统尚无足够资源，不予分配!");
                    flag=false;
                    break;
                }
            }
        }
        if(flag)
        {
            test(i);
            showdata();
            if(safe()!=1)
            {
                retest(i);
                showdata();
            }
        }
    }
    //主函数
    public static void main(String[] args) {
        Banker b=new Banker();
        Scanner sc=new Scanner(System.in);
        String choice;
        System.out.println("*************************************************************");
        System.out.println("                                                       银行家算法的实现  ");
        System.out.println("*************************************************************");
        b.init();
        b.showdata();
        if(b.safe()!=1)
        {
            System.exit(0);
        }

        do
        {
            System.out.println("*************************************************************");
            System.out.println("                     R(r):请求分配 ");
            System.out.println("                     E(e):退出        ");
            System.out.println("*************************************************************");
            System.out.print("请选择：");
            choice=sc.nextLine();
            switch(choice)
            {
                case "r":
                case "R":
                    b.bank();
                    break;
                case "e":
                case "E":
                    System.exit(0);//System.exit(0)是正常退出程序，System.exit(1)表示非正常退出程序。
                default:
                    System.out.println("请正确选择！");
                    break;
            }
        }while(choice!="");
    }
}