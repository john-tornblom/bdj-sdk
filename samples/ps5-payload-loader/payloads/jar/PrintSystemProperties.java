import java.util.Properties;

public class PrintSystemProperties {

    public static void main(String args[]) {
	Properties props = System.getProperties();
	props.list(System.out);
    }
}

