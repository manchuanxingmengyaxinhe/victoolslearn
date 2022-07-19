package hf.annotation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class test {
    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(1);
        list.add(1);
        list.add(1);
        list.add(1);
        list.add(1);
        list.add(1);
        list.add(1);
        Iterator<Integer> iterator = list.iterator();
        for (int i = 0; i < 3; i++){
            iterator.next();
        }
        list.iterator().forEachRemaining(x -> System.out.println(x));

    }


}
