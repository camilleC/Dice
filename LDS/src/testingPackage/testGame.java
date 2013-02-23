package testingPackage;
//import java.lang.*;
import static org.junit.Assert.*;
import csNetworking.Game;
import java.util.*;
import org.junit.Test;
public class testGame {
	

	// TODO Ask Dr. Reedy.  Do I need to do these?  The project is not on the build path
	// so I don't know why I need to clean up after it. 
	/*
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}*/

	   String temp[] = {" ", " "};
	    
	    @Test
	    public void testElapsedTime() throws InterruptedException {     
	        long startLobbyTime;
	        startLobbyTime =  System.currentTimeMillis();
	        Thread.sleep(10000); //is this 10 seconds? 
	        Game game = new Game(temp); 
	        assertEquals("result", game.getTimeToWait(), game.elapsedTime(startLobbyTime));
	        System.err.print("The unit test is not disabled. Check and see if it is in the build path.");
	        }
}

