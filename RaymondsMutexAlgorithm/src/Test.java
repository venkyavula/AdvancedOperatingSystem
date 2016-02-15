import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Test {

	public static void main(String[] args) {

		BufferedReader br = null;
		String userHome = System.getProperty( "user.home" );
		try {

			StringBuffer sb = new StringBuffer();
			String sCurrentLine;
			br = new BufferedReader(new FileReader(userHome+"/test.txt"));
			while ((sCurrentLine = br.readLine()) != null) {
				sb = sb.append(sCurrentLine);
			}

			//System.out.println(sb.toString());
			
			
			String testData = sb.toString();
			testData = testData.replaceAll("[^(|)]", ""); 	
			//System.out.println("testData");
			//System.out.println(testData);
			sb = new StringBuffer();
			
			System.out.println("Checking if file is valid: ");
			 if(isBalanced(testData)){
				 System.out.println("Strat1:SUCCESS");
				 
			 } else {
				 
				 System.out.println("Strat1:FAILURE");
			 }
			 


File folder = new File(userHome);
File[] listOfFiles = folder.listFiles();
int i  = 0;
for (File file : listOfFiles) {
    if (file.isFile()) {
        String fName = file.getName();
        if(fName.contains("failure")){
        	//System.out.println("Found failure file  : " + fName);
        	i = 1;
        } else if (fName.contains("Vector: FAILURE")){
        	//System.out.println("Found failure file  : " + fName);
        	i = 2;
        }
    }
}

if (i==0){
	System.out.println("Testing using vector clock SUCCESS! " );
}

else
{
 	System.out.println("Testing using vector clock.There is OVERLAP!!  " );
}

			 
			

		} catch (IOException io) {
			io.printStackTrace();
		}

	}
	
	 private static final Map<Character, Character> brackets = new HashMap<Character, Character>();
	    static {
	        brackets.put('(', ')');
	    }  

	
	 public static boolean isBalanced(String str) {
	        if (str.length() == 0) {
	            throw new IllegalArgumentException("String length should be greater than 0");
	        }
	        // odd number would always result in false
	        if ((str.length() % 2) != 0) {
	            return false;
	        }

	        final Stack<Character> stack = new Stack<Character>();
	        for (int i = 0; i < str.length(); i++) {
	            if (brackets.containsKey(str.charAt(i))) {
	                stack.push(str.charAt(i));
	            } else if (stack.empty() || (str.charAt(i) != brackets.get(stack.pop()))) {
	                return false;
	            } 
	        }
	        return true;
	    } 

}
