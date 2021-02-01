import java.util.Date;

public class RunnerTest {

    private String name;

    private RunnerTest subTest;

    public static void main(String[] args) {
        RunnerTest test = new RunnerTest();
        test.name = "topTest";
        test.subTest = new RunnerTest();
        test.subTest.name = "subTest";
        int i = 0;
        Date date = new Date();
        while (true) {
            i++;
            date = new Date();
            System.out.println(i);
            System.out.println(date.toGMTString());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
