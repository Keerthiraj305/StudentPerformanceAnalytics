public class Student {
    String name;
    int maths, science, english;

    public Student(String name, int m, int s, int e){
        this.name = name;
        this.maths = m;
        this.science = s;
        this.english = e;
    }

    public int getTotal(){
        return maths + science + english;
    }

    public double getAverage(){
        return getTotal() / 3.0;
    }

    public String getGrade(){
        double avg = getAverage();
        if (avg >= 90) return "A";
        if (avg >= 80) return "B";
        if (avg >= 70) return "C";
        if (avg >= 60) return "D";
        return "F";
    }
}