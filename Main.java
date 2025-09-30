import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Main {
    static List<ProcessControlBlock> blockedQueue = new ArrayList<>(); // I/O 대기 프로세스 큐
    // 프로세스 제어 블록 클래스
    public static class ProcessControlBlock {
        int pid, A, C, B, IO;
        int remainingTime;
        int waitingTime = 0;
        int turnaroundTime = 0;
        int finishingTime = 0;
        int CpuBurst;
        int CpuBurstCount = 0; // CPU 버스트 카운트
        int remainingCpuBurst = 0; // 남은 CPU 버스트 시간
        Random rand = new Random(); // 랜덤 객체 생성
        int IOBurst;                // I/O 버스트 시간
        int startTime = -1;         // 시작 시간, -1은 아직 시작하지 않았음을 의미
        boolean isStarted = false;  // 프로세스 시작 여부
        int IOtime = 0;             // I/O 시간 추가
        boolean isIo = false;       // I/O 진행 여부
        int remainingCpuTime;       // 남은 CPU 시간
        String state;               // 프로세스 상태 (NEW, READY, RUNNING, BLOCKED, TERMINATED)
        int currentIoBurst = 0;     // 현재 I/O 버스트 시간
        int currentCpuBurst;        // 현재 CPU 버스트 시간
        int cpuTime;                // CPU 시간 누적
        int fixedCpuBurst;
        int fixedIoBurst;

        public ProcessControlBlock(int pid, int A, int C, int B, int IO, Random rand) {   //process control block 만들기
            this.pid = pid;
            this.A = A;
            this.C = C;
            this.B = B;
            this.IO = IO;
            this.remainingTime = C;
            this.CpuBurst = (B > 0) ? rand.nextInt(B) + 1 : 0; // CPU Burst Time
            this.IOBurst = (IO > 0) ? rand.nextInt(IO) + 1 : 0; // 1부터 IO까지의 랜덤 값
        }

        @Override                  // 프로세스 정보 출력하기
        public String toString() {
            return String.format("PID %d: (A=%d, C=%d, B=%d, IO=%d)", pid, A, C, B, IO);
        } 
    }

    // 입력 파일에서 프로세스 정보 읽기
    public static List<ProcessControlBlock> readInputFile(String filename) throws FileNotFoundException { //exception 제거
        Scanner sc = new Scanner(new File(filename));
        StringBuilder rest = new StringBuilder();
        while (sc.hasNextLine()) {
            rest.append(sc.nextLine());
            
        }
        sc.close();

        Pattern pattern = Pattern.compile("\\((\\d+) (\\d+) (\\d+) (\\d+)\\)"); //어떻게 프로세스 정보를 읽어올지 정규표현식으로 정의
        Matcher matcher = pattern.matcher(rest.toString());      //정규표현식으로 읽어온 프로세스 정보를 매칭
        List<ProcessControlBlock> pcbList = new ArrayList<>();
        int pid = 0; //프로세스 ID 순서
        while (matcher.find()) {
            int A = Integer.parseInt(matcher.group(1));  //matcher.group(1)에서 A값을 읽어옴
            int C = Integer.parseInt(matcher.group(2));  //matcher.group(2)에서 C값을 읽어옴
            int B = Integer.parseInt(matcher.group(3));  //matcher.group(3)에서 B값을 읽어옴
            int IO = Integer.parseInt(matcher.group(4)); //matcher.group(4)에서 IO값을 읽어옴
            pcbList.add(new ProcessControlBlock(pid++, A, C, B, IO, new Random()));
        }

        
        return pcbList;
    }
//-----------------------------------------------------------------------------------------------*/
    // FCFS 시뮬레이션 실행
    public static void runFCFS(List<ProcessControlBlock> processes) {
    Queue<ProcessControlBlock> readyQueue = new LinkedList<>(); // 준비 큐
    Random rand = new Random();

    int currentTime = 0;
    int finished = 0;
    ProcessControlBlock current = null;

    for (ProcessControlBlock pcb : processes) {
        pcb.remainingCpuTime = pcb.C;
        pcb.state = "NEW";
        pcb.fixedCpuBurst = (pcb.B > 0) ? rand.nextInt(pcb.B) + 1 : 1;  // CPU 버스트 시간
        pcb.fixedIoBurst = (pcb.IO > 0) ? rand.nextInt(pcb.IO) + 1 : 0; // I/O 버스트 시간
    }

    while (finished < processes.size()) {
        List<ProcessControlBlock> arrivals = new ArrayList<>();  // 도착한 프로세스들을 저장할 리스트
        for (ProcessControlBlock pcb : processes) {
            if (pcb.A == currentTime) {
                pcb.state = "READY";
                arrivals.add(pcb);
            }
        }
        arrivals.sort(Comparator.comparingInt(p -> p.pid));
        readyQueue.addAll(arrivals);

        if (current == null && !readyQueue.isEmpty()) {
            current = readyQueue.poll();
            current.state = "RUNNING";
            if (current.startTime == -1) current.startTime = currentTime;
            current.currentCpuBurst = Math.min(current.fixedCpuBurst, current.remainingCpuTime);
            current.CpuBurstCount++;
        }

        if (current != null) {
            if (current.state.equals("RUNNING")) {
                current.remainingCpuTime--;
                current.currentCpuBurst--;
                current.cpuTime++;

                if (current.remainingCpuTime == 0) {
                    current.finishingTime = currentTime + 1;
                    current.turnaroundTime = current.finishingTime - current.A;
                    current.state = "TERMINATED";
                    current = null;
                    finished++;
                } else if (current.currentCpuBurst == 0) {
                    if (current.fixedIoBurst > 0) {
                        current.state = "BLOCKED";
                        current.currentIoBurst = current.fixedIoBurst;
                    } else {
                        current.state = "RUNNING";
                        current.currentCpuBurst = Math.min(current.fixedCpuBurst, current.remainingCpuTime);
                        current.CpuBurstCount++;
                    }
                }
            } else if (current.state.equals("BLOCKED")) {
                current.currentIoBurst--;
                current.IOtime++;
                if (current.currentIoBurst == 0) {
                    current.state = "RUNNING";
                    current.currentCpuBurst = Math.min(current.fixedCpuBurst, current.remainingCpuTime);
                    current.CpuBurstCount++;
                }
            }
        }

        for (ProcessControlBlock pcb : readyQueue) {
            if (pcb.state.equals("READY")) {
                pcb.waitingTime++;
            }
        }

        currentTime++;
    }

    System.out.println("\nFCFS 결과 (각 프로세스별):");
    System.out.printf("PID\t(A,C,B,IO)\tFinish\tTurnaround\tCPU\tIO\tWaiting\n");
    for (ProcessControlBlock pcb : processes) {
        System.out.printf("%d\t(%d,%d,%d,%d)\t%d\t%d\t\t%d\t%d\t%d\n",
            pcb.pid, pcb.A, pcb.C, pcb.B, pcb.IO,
            pcb.finishingTime, pcb.turnaroundTime,
            pcb.cpuTime, pcb.IOtime, pcb.waitingTime);
    }
}







//-----------------------------------------------------------------------------------------------*/

//-----------------------------------------------------------------------------------------------*/

    // RR 시뮬레이션 실행
    // timeQuantum: 1, 10, 100 중 하나
    public static void runRR(List<ProcessControlBlock> processes, int timeQuantum) {
    Queue<ProcessControlBlock> readyQueue = new LinkedList<>();
    List<ProcessControlBlock> blockedQueue = new ArrayList<>();
    Random rand = new Random();

    int currentTime = 0;
    int finished = 0;
    ProcessControlBlock current = null;
    int timeSlice = 0;

    for (ProcessControlBlock pcb : processes) {
        pcb.remainingCpuTime = pcb.C;
        pcb.state = "NEW";
        pcb.fixedCpuBurst = (pcb.B > 0) ? rand.nextInt(pcb.B) + 1 : 1;
        pcb.fixedIoBurst = (pcb.IO > 0) ? rand.nextInt(pcb.IO) + 1 : 0;
    }

    while (finished < processes.size()) {
        for (ProcessControlBlock pcb : processes) {
            if (pcb.A == currentTime) {
                pcb.state = "READY";
                readyQueue.add(pcb);
            }
        }

        List<ProcessControlBlock> unblocked = new ArrayList<>();
        for (ProcessControlBlock pcb : blockedQueue) {
            pcb.currentIoBurst--;
            if (pcb.currentIoBurst == 0) {
                pcb.state = "READY";
                readyQueue.add(pcb);
                unblocked.add(pcb);
            }
        }
        blockedQueue.removeAll(unblocked);

        if ((current == null || timeSlice == timeQuantum) && !readyQueue.isEmpty()) {
            if (current != null && current.remainingCpuTime > 0) {
                current.state = "READY";
                readyQueue.add(current);
            }
            current = readyQueue.poll();
            current.state = "RUNNING";
            if (current.startTime == -1) current.startTime = currentTime;
            if (current.currentCpuBurst == 0) {
                current.currentCpuBurst = Math.min(current.fixedCpuBurst, current.remainingCpuTime);
            }
            timeSlice = 0;
        }

        if (current != null) {
            current.currentCpuBurst--;
            current.remainingCpuTime--;
            timeSlice++;

            if (current.remainingCpuTime == 0) {
                current.finishingTime = currentTime + 1;
                current.turnaroundTime = current.finishingTime - current.A;
                current.state = "TERMINATED";
                current = null;
                timeSlice = 0;
                finished++;
            } else if (current.currentCpuBurst == 0) {
                if (current.fixedIoBurst > 0) {
                    current.currentIoBurst = current.fixedIoBurst;
                    current.state = "BLOCKED";
                    blockedQueue.add(current);
                    current = null;
                    timeSlice = 0;
                } else {
                    current.currentCpuBurst = Math.min(current.fixedCpuBurst, current.remainingCpuTime);
                    timeSlice = 0;
                }
            }
        }

        for (ProcessControlBlock pcb : processes) {
            if (pcb.state.equals("TERMINATED")) continue;
            switch (pcb.state) {
                case "RUNNING": pcb.cpuTime++; break;
                case "BLOCKED": pcb.IOtime++; break;
                case "READY": pcb.waitingTime++; break;
            }
        }

        currentTime++;
    }

    System.out.println("\nRR 결과 (각 프로세스별):");
    System.out.printf("PID\t(A,C,B,IO)\tFinish\tTurnaround\tCPU\tIO\tWaiting\n");
    for (ProcessControlBlock pcb : processes) {
        System.out.printf("%d\t(%d,%d,%d,%d)\t%d\t%d\t\t%d\t%d\t%d\n",
            pcb.pid, pcb.A, pcb.C, pcb.B, pcb.IO,
            pcb.finishingTime, pcb.turnaroundTime,
            pcb.cpuTime, pcb.IOtime, pcb.waitingTime);
    }
}





//-----------------------------------------------------------------------------------------------*/
    // SJF 시뮬레이션 실행, 비선점

    public static void runSJF(List<ProcessControlBlock> processes) {
    Queue<ProcessControlBlock> readyQueue = new LinkedList<>();
    Random rand = new Random();

    int currentTime = 0;
    int finished = 0;
    ProcessControlBlock current = null;

    for (ProcessControlBlock pcb : processes) {
        pcb.remainingCpuTime = pcb.C;
        pcb.state = "NEW";
        pcb.fixedCpuBurst = (pcb.B > 0) ? rand.nextInt(pcb.B) + 1 : 1;
        pcb.fixedIoBurst = (pcb.IO > 0) ? rand.nextInt(pcb.IO) + 1 : 0;
        System.out.println("PID " + pcb.pid + " fixedCpuBurst = " + pcb.fixedCpuBurst + ", fixedIoBurst = " + pcb.fixedIoBurst);
    }

    while (finished < processes.size()) {
        List<ProcessControlBlock> arrivals = new ArrayList<>();
        for (ProcessControlBlock pcb : processes) {
            if (pcb.A == currentTime) {
                pcb.state = "READY";
                arrivals.add(pcb);
            }
        }
        arrivals.sort(Comparator.comparingInt(p -> p.pid));
        readyQueue.addAll(arrivals);

        if (current != null && current.state.equals("BLOCKED")) {
            current.currentIoBurst--;
            System.out.println("TIME " + currentTime + ": PID " + current.pid + " BLOCKED, IO remaining: " + current.currentIoBurst);
            if (current.currentIoBurst == 0) {
                current.state = "RUNNING";
                current.currentCpuBurst = Math.min(current.fixedCpuBurst, current.remainingCpuTime);
                current.CpuBurstCount++;
            }
        }

        if (current == null && !readyQueue.isEmpty()) {
            current = readyQueue.stream()
                .min(Comparator.comparingInt(p -> p.remainingCpuTime))
                .orElse(null);
            readyQueue.remove(current);
            current.state = "RUNNING";
            if (current.startTime == -1) current.startTime = currentTime;
            current.currentCpuBurst = Math.min(current.fixedCpuBurst, current.remainingCpuTime);
            current.CpuBurstCount++;
            for (ProcessControlBlock pcb : processes) {
                if (pcb == current) pcb.state = "RUNNING";
            }
            System.out.println("TIME " + currentTime + ": PID " + current.pid + " START RUNNING, CPU burst = " + current.currentCpuBurst);
        }

        if (current != null && current.state.equals("RUNNING")) {
            if (current.currentCpuBurst == 0) {
                current.currentCpuBurst = Math.min(current.fixedCpuBurst, current.remainingCpuTime);
                current.CpuBurstCount++;
            }
            current.remainingCpuTime--;
            current.currentCpuBurst--;
            System.out.println("TIME " + currentTime + ": PID " + current.pid + " EXECUTING, remaining: " + current.remainingCpuTime);

            if (current.remainingCpuTime == 0) {
                current.finishingTime = currentTime + 1;
                current.turnaroundTime = current.finishingTime - current.A;
                current.state = "TERMINATED";
                current = null;
                finished++;
            } else if (current.currentCpuBurst == 0) {
                if (current.fixedIoBurst > 0) {
                    current.state = "BLOCKED";
                    current.currentIoBurst = current.fixedIoBurst;
                } else {
                    current.state = "RUNNING";
                    current.currentCpuBurst = Math.min(current.fixedCpuBurst, current.remainingCpuTime);
                    current.CpuBurstCount++;
                }
            }
        }

        if (current != null && current.state.equals("RUNNING")) {
            current.cpuTime++;
        }

        if (current != null && current.state.equals("RUNNING")) {
            current.cpuTime++;
        }
        for (ProcessControlBlock pcb : readyQueue) {
            pcb.waitingTime++;
        }
        for (ProcessControlBlock pcb : processes) {
            if (pcb.state.equals("BLOCKED")) {
                pcb.IOtime++;
            }
        }

        currentTime++;
    }

    System.out.println("\nSJF 결과 (각 프로세스별):");
    System.out.printf("PID\t(A,C,B,IO)\tFinish\tTurnaround\tCPU\tIO\tWaiting\n");
    for (ProcessControlBlock pcb : processes) {
        System.out.printf("%d\t(%d,%d,%d,%d)\t%d\t%d\t\t%d\t%d\t%d\n",
            pcb.pid, pcb.A, pcb.C, pcb.B, pcb.IO,
            pcb.finishingTime, pcb.turnaroundTime,
            pcb.cpuTime, pcb.IOtime, pcb.waitingTime);
    }
}
//-----------------------------------------------------------------------------------------------*/
    // 요약 정보 출력
    public static void summary(List<ProcessControlBlock> processes) {
    int totalTurnaroundTime = 0;
    int totalWaitingTime = 0;
    int totalCpuTime = 0;
    int totalIoTime = 0;
    int finishingTime = 0;

    for (ProcessControlBlock pcb : processes) {
        totalTurnaroundTime += pcb.turnaroundTime;
        totalWaitingTime += pcb.waitingTime;
        totalCpuTime += pcb.cpuTime;
        totalIoTime += pcb.IOtime;
        if (pcb.finishingTime > finishingTime) {
            finishingTime = pcb.finishingTime;
        }
    }

    System.out.println("\n=== Summary ===");
    System.out.println("Finishing Time: " + finishingTime);
    System.out.printf("CPU utilization: %.2f%%\n", finishingTime > 0 ? 100.0 * totalCpuTime / finishingTime : 0);  // CPU 사용률 소수점2
    System.out.printf("I/O utilization: %.2f%%\n", finishingTime > 0 ? 100.0 * totalIoTime / finishingTime : 0);   // I/O 사용률 소수점2
    System.out.printf("Throughput: %.3f processes per 100 time units\n", 
        finishingTime > 0 ? 100.0 * processes.size() / finishingTime : 0);
    System.out.printf("평균(Average) Turnaround Time: %.1f\n", 
        processes.size() > 0 ? (double)totalTurnaroundTime / processes.size() : 0);
    System.out.printf("평균(Average) Waiting Time: %.1f\n", 
        processes.size() > 0 ? (double)totalWaitingTime / processes.size() : 0);
}

    //------------------------------------------------------------------------------------------------*/
    // 메인 함수
    public static void main(String[] args) throws FileNotFoundException {

        String filename = "input.txt";
        List<ProcessControlBlock> pcbs = readInputFile(filename);
        System.out.println("원하시는 스케쥴링 알고리즘을 고르시오.: (1. FCFS, 2. SJF, 3. RR)");
        Scanner sc = new Scanner(System.in);
        int choice = sc.nextInt(); //사용자 입력 정수값으로 받기
        if (choice < 1 || choice > 3) { // 1, 2, 3 중 하나가 아니면
            System.out.println("잘못된 선택입니다. 1, 2, 3 중 하나를 선택하세요.");
            return;
        }
        switch (choice) {
            case 1:
                System.out.println("FCFS 스케쥴링을 선택하셨습니다.");
                runFCFS(pcbs);
                summary(pcbs);
                break;
            case 2:
                System.out.println("SJF 스케쥴링을 선택하셨습니다.");
                runSJF(pcbs);
                summary(pcbs);
                
                break;
            case 3:
                System.out.println("RR 스케쥴링을 선택하셨습니다.");
                // runRR(pcbs);
                System.out.println("time quantum을 입력하세요: (1, 10, 100)");
                int timeQuantum = sc.nextInt();
                if (timeQuantum != 1 && timeQuantum != 10 && timeQuantum != 100) {
                    System.out.println("잘못된 time quantum입니다.");
                    return;
                }
                if (timeQuantum == 1){
                    runRR(pcbs, timeQuantum);
                    summary(pcbs);
                }
                else if (timeQuantum == 10){
                    runRR(pcbs, timeQuantum);
                    summary(pcbs);
                }
                else if (timeQuantum == 100){
                    runRR(pcbs, timeQuantum);
                    summary(pcbs);
                }
                break;
                
            default:
                System.out.println("잘못된 선택입니다.");
                return;
        }

        
        System.out.println("프로세스 스케쥴링이 완료되었습니다.");
        
    }
}