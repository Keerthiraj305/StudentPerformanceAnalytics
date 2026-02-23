import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {

        ArrayList<Student> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("student_marks.csv"))) {
            String line;

            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                list.add(new Student(
                    data[0],
                    Integer.parseInt(data[1]),
                    Integer.parseInt(data[2]),
                    Integer.parseInt(data[3])
                ));
            }
        }

        if (list.isEmpty()) {
            System.out.println("No student data found.");
            return;
        }

        int totalMath=0,totalSci=0,totalEng=0;
        int pass=0;
        Student topper = list.get(0);

        for(Student s : list){
            totalMath += s.maths;
            totalSci += s.science;
            totalEng += s.english;

            if(s.getTotal() >= 120)
                pass++;

            if(s.getTotal() > topper.getTotal())
                topper = s;
        }

        System.out.println("Average Maths: " + totalMath/list.size());
        System.out.println("Average Science: " + totalSci/list.size());
        System.out.println("Average English: " + totalEng/list.size());
        System.out.println("Topper: " + topper.name);
        System.out.println("Pass %: " + (pass*100/list.size()));

        new DashboardUI(list);
    }
}