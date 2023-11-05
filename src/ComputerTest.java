/*
 * Unit tests for the Computer class.
 */

package src;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import src.Computer;

/**
 * @author Alan Fowler
 * @author Avinash Bavisetty
 * @author Jaskaran Toor
 * @version 1.3
 */
class ComputerTest {

    // An instance of the Computer class to use in the tests.
    private Computer myComputer;

    @BeforeEach
    void setUp() {
        myComputer = new Computer();
    }




    /*
     * NOTE:
     * Programs in unit tests should ideally have one instruction per line
     * with a comment for each line.
     */

    /**
     * Test method for {@link Computer#executeBranch()}.
     */
    @Test
    void testExecuteBranch() {
        String[] program = {
                // Use a loop
                // Add 1 + 2 + 3 + 4 + 5 + 6 in a loop = 21
                "0001001001100101", //ADD - R1 <- R1 + 5
                "0001000000000001", //ADD the count to the sum
                "0001001001111111", //decrement the count
                "0000001111111101", //BRANCH while the loop count is positive
                "1111000000100101"};// Trap out

        myComputer.display();
        myComputer.loadMachineCode(program);
        myComputer.execute();

        //Check R1 still has the value of 0
        assertEquals(21, myComputer.getRegisters()[0].getUnsignedValue());

        BitString expectedCC = new BitString();
        expectedCC.setBits("010".toCharArray());
        assertEquals(expectedCC.get2sCompValue(), myComputer.getCC().get2sCompValue());



    }

    /**
     * Test method for {@link Computer#executeLoad()}.
     */
    @Test
    void testExecuteLoad() {
        String[] program =
                {"0010001000000001", 		// LOAD
                        "1111000000100101",     	// TRAP HALT
                        "0000000000000101"};		// #5

        myComputer.loadMachineCode(program);
        myComputer.execute();

        //Check registers
        assertEquals(5, myComputer.getRegisters()[1].get2sCompValue());

        //Check condition codes
        BitString expectedCC = new BitString();
        expectedCC.setBits("001".toCharArray());
        assertEquals(expectedCC.get2sCompValue(), myComputer.getCC().get2sCompValue());
    }


    /**
     * Test method for {@link Computer#executeLoad()}.
     */
    @Test
    void testExecute2Load() {
        String[] program =
                {"0010000000000001",	 // LOAD
                        "1111000000100101",      //TRAP HALT
                        "0000000000010000"};	// #16

        myComputer.loadMachineCode(program);
        myComputer.execute();

        //Check registers
        assertEquals(16, myComputer.getRegisters()[0].get2sCompValue());

        //Check condition codes
        BitString expectedCC = new BitString();
        expectedCC.setBits("001".toCharArray());
        assertEquals(expectedCC.get2sCompValue(), myComputer.getCC().get2sCompValue());
    }

    /**
     * Test method for {@link Computer#executeAnd()}.
     */
    @Test
    void testExecuteAnd() {
        String[] program =
                {"0101000001000011",  // R0 <- R1 AND R3
                        "1111000000100101"}; // HALT

        myComputer.loadMachineCode(program);
        myComputer.execute();

        assertEquals(1, myComputer.getRegisters()[0].get2sCompValue());

        // Check that CC was set correctly
        BitString expectedCC = new BitString();
        expectedCC.setBits("001".toCharArray());
        assertEquals(expectedCC.get2sCompValue(), myComputer.getCC().get2sCompValue());
    }

    /**
     * Test method for {@link Computer#executeNot()}.
     */
    @Test
    void testExecuteNot5() {

        //myComputer.display();

        // NOTE: R5 contains #5 initially when the Computer is instantiated
        // So, iF we execute R4 <- NOT R5, then R4 should contain 1111 1111 1111 1010    (-6)
        // AND CC should be 100

        String program[] = {
                "1001100101111111",    // R4 <- NOT R5
                "1111000000100101"     // TRAP - vector x25 - HALT
        };

        myComputer.loadMachineCode(program);
        myComputer.execute();

        assertEquals(-6, myComputer.getRegisters()[4].get2sCompValue());

        // Check that CC was set correctly
        BitString expectedCC = new BitString();
        expectedCC.setBits("100".toCharArray());
        assertEquals(expectedCC.get2sCompValue(), myComputer.getCC().get2sCompValue());

        //myComputer.display();
    }

    /**
     * Test method for {@link Computer#executeAdd()}. <br>
     * Computes 2 + 2. R0 <- R2 + R2
     */
    @Test
    void testExecuteAddR2PlusR2() {

        String[] program =
                {"0001000010000010",  // R0 <- R2 + R2 (#4)
                        "1111000000100101"}; // HALT

        myComputer.loadMachineCode(program);
        myComputer.execute();

        assertEquals(4, myComputer.getRegisters()[0].get2sCompValue());

        // Check that CC was set correctly
        BitString expectedCC = new BitString();
        expectedCC.setBits("001".toCharArray());
        assertEquals(expectedCC.get2sCompValue(), myComputer.getCC().get2sCompValue());
    }

    /**
     * Test method for {@link Computer#executeAdd()}. <br>
     * Computes 2 + 3. R0 <- R2 + #3
     */
    @Test
    void testExecuteAddR2PlusImm3() {

        String[] program =
                {"0001000010100011",  // R0 <- R2 + #3
                        "1111000000100101"}; // HALT

        myComputer.loadMachineCode(program);
        myComputer.execute();

        assertEquals(5, myComputer.getRegisters()[0].get2sCompValue());

        // Check that CC was set correctly
        BitString expectedCC = new BitString();
        expectedCC.setBits("001".toCharArray());
        assertEquals(expectedCC.get2sCompValue(), myComputer.getCC().get2sCompValue());
    }

    /**
     * Test method for {@link Computer#executeAdd()}. <br>
     * Computes 2 - 3. R0 <- R2 + #-3
     */
    @Test
    void testExecuteAddR2PlusImmNeg3() {

        String[] program =
                {"0001000010111101",  // R0 <- R2 + #-3
                        "1111000000100101"}; // HALT

        myComputer.loadMachineCode(program);
        myComputer.execute();

        assertEquals(-1, myComputer.getRegisters()[0].get2sCompValue());

        // Check that CC was set correctly
        BitString expectedCC = new BitString();
        expectedCC.setBits("100".toCharArray());
        assertEquals(expectedCC.get2sCompValue(), myComputer.getCC().get2sCompValue());
    }

}