/**
 *
 * Java program which demonstrate that we can not override static method in Java.
 * Had Static method can be overridden, with Super class type and sub class object
 * static method from sub class would be called in our example, which is not the case.
 * @author
 */
public class StaticDemo {
 
    public static void main(String args[]) {
     
     System.out.println(1.0/0.0);
        //if we can  override static , this should call method from Child class
     if('y'=='y'){
    	 System.out.println("true");
     }
     
    } 
 
}

