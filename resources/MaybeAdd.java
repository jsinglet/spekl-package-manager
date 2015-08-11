
//
// This program contains two errors. Can you find them?
//
public class MaybeAdd {

    FooClass a;
    
     //@ requires a > 0;
     //@ requires b > 0;
     //@ ensures \result == a+b;
     public static int add(int a, int b){
         return a-b;
     }
    

    public static void main(String args[]){
         System.out.println(add(2,3));
     }
 }

