package src;

import java.util.Arrays;

/**
 * The Computer class is composed of registers, memory, PC, IR, and CC.
 * The Computer can execute a program based on the the instructions in memory.
 *
 * @author mmuppa
 * @author acfowler
 * @author Avinash Bavisetty
 * @author Jaskaran Toor
 * @version 1.5
 */
public class Computer {

    private final static int MAX_MEMORY = 50;
    private final static int MAX_REGISTERS = 8;

    private BitString mRegisters[];
    private BitString mMemory[];
    private BitString mPC;
    private BitString mIR;
    private BitString mCC;

    /**
     * Initialize all memory addresses to 0, registers to 0 to 7
     * PC, IR to 16 bit 0s and CC to 000.
     */
    public Computer() {
        mPC = new BitString();
        mPC.setUnsignedValue(0);
        mIR = new BitString();
        mIR.setUnsignedValue(0);
        mCC = new BitString();
        mCC.setBits(new char[] { '0', '0', '0' });

        mRegisters = new BitString[MAX_REGISTERS];
        for (int i = 0; i < MAX_REGISTERS; i++) {
            mRegisters[i] = new BitString();
            mRegisters[i].setUnsignedValue(i);
        }

        mMemory = new BitString[MAX_MEMORY];
        for (int i = 0; i < MAX_MEMORY; i++) {
            mMemory[i] = new BitString();
            mMemory[i].setUnsignedValue(0);
        }
    }

    // The public accessor methods shown below are useful for unit testing.
    // Do NOT add public mutator methods (setters)!

    /**
     * @return the registers
     */
    public BitString[] getRegisters() {
        return copyBitStringArray(mRegisters);
    }

    /**
     * @return the memory
     */
    public BitString[] getMemory() {
        return copyBitStringArray(mMemory);
    }

    /**
     * @return the PC
     */
    public BitString getPC() {
        return mPC.copy();
    }

    /**
     * @return the IR
     */
    public BitString getIR() {
        return mIR.copy();
    }

    /**
     * @return the CC
     */
    public BitString getCC() {
        return mCC.copy();
    }

    /**
     * Safely copies a BitString array.
     * @param theArray the array to copy.
     * @return a copy of theArray.
     */
    private BitString[] copyBitStringArray(final BitString[] theArray) {
        BitString[] bitStrings = new BitString[theArray.length];
        Arrays.setAll(bitStrings, n -> bitStrings[n] = theArray[n].copy());
        return bitStrings;
    }

    /**
     * Loads a 16 bit word into memory at the given address.
     * @param address memory address
     * @param word data or instruction or address to be loaded into memory
     */
    private void loadWord(int address, BitString word) {
        if (address < 0 || address >= MAX_MEMORY) {
            throw new IllegalArgumentException("Invalid address");
        }
        mMemory[address] = word;
    }

    /**
     * Loads a machine code program, as Strings.
     * @param theWords the Strings that contain the instructions or data.
     */
    public void loadMachineCode(final String ... theWords) {
        if (theWords.length == 0 || theWords.length >= MAX_MEMORY) {
            throw new IllegalArgumentException("Invalid words");
        }
        for (int i = 0; i < theWords.length; i++) {
            final BitString instruction = new BitString();
            instruction.setBits(theWords[i].toCharArray());
            loadWord(i, instruction);
        }
    }


    // The next 6 methods are used to execute the required instructions:
    // BR, ADD, LD, AND, NOT, TRAP

    /**
     * op   nzp pc9offset
     * 0000 000 000000000
     *
     * The condition codes specified by bits [11:9] are tested.
     * If bit [11] is 1, N is tested; if bit [11] is 0, N is not tested.
     * If bit [10] is 1, Z is tested, etc.
     * If any of the condition codes tested is 1, the program branches to the memory location specified by
     * adding the sign-extended PCoffset9 field to the incremented PC.
     */
    public void executeBranch() {
        BitString nzp = mIR.substring(4, 3);
        char[] nzpBits  = nzp.getBits();
        char[] conditionCodeBits = mCC.getBits();
        boolean check = false;
        for (int i = 0; i <= 2; i++) {
            if (nzpBits[i] == '1' && conditionCodeBits[i] == '1') {
                check = true;
            }
        }
        // Skips lines of instructions if the BR and CC match.
        if (check) {
            BitString lineSkip = mIR.substring(7, 9);
            int lineSkipValue = lineSkip.get2sCompValue();
            // This is wrong.
            mPC.set2sCompValue(mPC.get2sCompValue() + lineSkipValue);
        }
    }


    /**
     * op   dr  sr1      sr2
     * 0001 000 000 0 00 000
     *
     * OR
     *
     * op   dr  sr1   imm5
     * 0001 000 000 1 00000
     *
     * If bit [5] is 0, the second source operand is obtained from SR2.
     * If bit [5] is 1, the second source operand is obtained by sign-extending the imm5 field to 16 bits.
     * In both cases, the second source operand is added to the contents of SR1 and the
     * result stored in DR. The condition codes are set, based on whether the result is
     * negative, zero, or positive.
     */
    public void executeAdd() {

        // implement the ADD instruction here
        BitString destBS = mIR.substring(4, 3);
        BitString sourceBS1 = mIR.substring(7, 3);
        BitString sourceBS2 = mIR.substring(13, 3);

        if (mIR.substring(10,1).getUnsignedValue() == 1) {
            mRegisters[sourceBS2.getUnsignedValue()] = mIR.substring(11, 5);
        }
        mRegisters[destBS.getUnsignedValue()]
                .set2sCompValue(getSum(mRegisters[sourceBS1.getUnsignedValue()],
                        mRegisters[sourceBS2.getUnsignedValue()]));

        //BitString b = mRegisters[destBS.getUnsignedValue()];
        //setConditionCode(b.get2sCompValue());
        mCC.setBits(setConditionCode(mRegisters[destBS.getUnsignedValue()].get2sCompValue()));

    }

    private int getSum(BitString sourceBS1, BitString sourceBS2) {
        return sourceBS1.get2sCompValue() + sourceBS2.get2sCompValue();

    }

    /**
     * Performs the load operation by placing the data from PC
     * + PC offset9 bits [8:0]
     * into DR - bits [11:9]
     * then sets CC.
     */
    public void executeLoad() {
        BitString destBS = mIR.substring(4, 3);
        BitString offsetBS2 = mIR.substring(7, 9);


        int location = getPC().get2sCompValue() + offsetBS2.get2sCompValue();
        mRegisters[destBS.getUnsignedValue()].set2sCompValue(getMemory()[location].get2sCompValue());


        // Set condition codes
        mCC.setBits(setConditionCode(mRegisters[destBS.getUnsignedValue()].get2sCompValue()));
    }

    /**
     * This method sets the condition codes
     * @param num
     * @return
     */
    private char[] setConditionCode(int num) {

        // Properties
        char[] code = new char[3];

        // Determine if the condition codes should be N, Z, or P
        if(num < 0 ) {
            code[0] = '1';
            code[1] = '0';
            code[2] = '0';
        } else if (num > 0) {
            code[0] = '0';
            code[1] = '0';
            code[2] = '1';
        } else {
            code[0] = '0';
            code[1] = '1';
            code[2] = '0';
        }

        return code;
    }

    /**
     * op   dr  sr1      sr2
     * 0101 000 000 0 00 000
     *
     * OR
     *
     * op   dr  sr1   imm5
     * 0101 000 000 1 00000
     *
     * If bit [5] is 0, the second source operand is obtained from SR2.
     * If bit [5] is 1, the second source operand is obtained by sign-extending the imm5 field to 16 bits.
     * In either case, the second source operand and the contents of SR1 are bitwise ANDed
     * and the result stored in DR.
     * The condition codes are set, based on whether the binary value produced, taken as a 2’s complement integer,
     * is negative, zero, or positive.
     */
    public void executeAnd() {
        BitString directory = mIR.substring(4, 3);
        BitString BS1 = mIR.substring(7, 3);
        BitString value = mIR.substring(10, 1);
        int condition_value;

        if (value.getUnsignedValue() == 0) {
            condition_value = mRegisters[BS1.getUnsignedValue()].get2sCompValue()
                    & mRegisters[mIR.substring(13, 3).getUnsignedValue()].get2sCompValue();

            mRegisters[directory.getUnsignedValue()].set2sCompValue(condition_value);
        }

        else {
            condition_value = mRegisters[BS1.getUnsignedValue()].get2sCompValue()
                    & mRegisters[mIR.substring(11,5).getUnsignedValue()].get2sCompValue();

            mRegisters[directory.getUnsignedValue()].set2sCompValue(condition_value);
        }

        mCC.setBits(setConditionCode(condition_value));
    }

    /**
     * Performs not operation by using the data from the source register (bits[8:6])
     * and inverting and storing in the destination register (bits[11:9]).
     * Then sets CC.
     */
    public void executeNot() {
        BitString destBS = mIR.substring(4, 3);
        BitString sourceBS = mIR.substring(7, 3);
        mRegisters[destBS.getUnsignedValue()] = mRegisters[sourceBS.getUnsignedValue()].copy();
        mRegisters[destBS.getUnsignedValue()].invert();

        // add code here to set the condition code
        mCC.setBits(setConditionCode(mRegisters[destBS.getUnsignedValue()].get2sCompValue()));
    }

    /**
     * Executes the trap operation by checking the vector (bits [7:0]
     *
     * vector x21 - OUT
     * vector x25 - HALT
     *
     * @return true if this Trap is a HALT command; false otherwise.
     */
    public boolean executeTrap() {
        boolean halt = true;
        boolean out = false;
        BitString outBS = new BitString();

        // implement the TRAP instruction here
        outBS.setBits("00100001".toCharArray());
        if (mIR.substring(8,8).get2sCompValue() == outBS.get2sCompValue()) {
            System.out.print((char) getRegisters()[0].get2sCompValue());
            return out;
        }
        return halt;
    }


    /**
     * This method will execute all the instructions starting at address 0
     * until a HALT instruction is encountered.
     */
    public void execute() {
        BitString opCodeStr;
        int opCode;
        boolean halt = false;

        while (!halt) {
            // Fetch the next instruction
            mIR = mMemory[mPC.getUnsignedValue()];
            // increment the PC
            mPC.addOne();

            // Decode the instruction's first 4 bits
            // to figure out the opcode
            opCodeStr = mIR.substring(0, 4);
            opCode = opCodeStr.getUnsignedValue();

            // What instruction is this?
            if (opCode == 0) { // BR
                executeBranch();
            } else if (opCode == 1) {  // ADD    0001
                executeAdd();
            } else if (opCode == 2) {  // LD     0010
                executeLoad();
            } else if (opCode == 5) {  // AND    0101
                executeAnd();
            } else if (opCode == 9) {  // NOT    1001
                executeNot();
            } else if (opCode == 15) { // TRAP   1111
                halt = executeTrap();
            } else {
                throw new UnsupportedOperationException("Illegal opCode: " + opCode);
            }
        }
    }

    /**
     * Displays the computer's state
     */
    public void display() {
        System.out.println();
        System.out.print("PC ");
        mPC.display(true);
        System.out.print("   ");

        System.out.print("IR ");
        mPC.display(true);
        System.out.print("   ");

        System.out.print("CC ");
        mCC.display(true);
        System.out.println("   ");
        for (int i = 0; i < MAX_REGISTERS; i++) {
            System.out.printf("R%d ", i);
            mRegisters[i].display(true);
            if (i % 3 == 2) {
                System.out.println();
            } else {
                System.out.print("   ");
            }
        }
        System.out.println();
        for (int i = 0; i < MAX_MEMORY; i++) {
            System.out.printf("%3d ", i);
            mMemory[i].display(true);
            if (i % 3 == 2) {
                System.out.println();
            } else {
                System.out.print("   ");
            }
        }
        System.out.println();
        System.out.println();
    }
}