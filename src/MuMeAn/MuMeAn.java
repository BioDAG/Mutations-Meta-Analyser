package MuMeAn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
//import java.util.StringTokenizer;

/**
 *
 * @author thanos
 */
public class MuMeAn {

    /**
     * *************************************
     * utility functions ******************************************
     */
    /**
     * @param input
     * @param delimiter
     * @return output Takes a line and a delimeter and returns an array
     * containing the line's fields
     */
    public static ArrayList<String> fastSplitter(String input, String delimiter) { //Thanoss fastSplitter ommited the last element always, Bug?
        ArrayList<String> output = new ArrayList<String>(10);
        //split using the delimiter
        String[] lineSplit = input.split(delimiter);
        for (String field : lineSplit) {
            // also returns empty fields, keep an eye on that
            output.add(field);
        }
        return output;

    }

    /**
     * @param input
     * @return output For the input line (which is a line of qualities) it
     * returns an array where each element is a quality
     */
    public static ArrayList<String> fastArrayFormer(String input) {
        ArrayList<String> output = new ArrayList<>(300);
        //remove the charachters [ and ]
        String cleanInput = input.replaceAll("[\\[\\]]", "");
        //get the fields using dilimeter 
        output = fastSplitter(cleanInput, ", ");
        return output;

    }

    /**
     * @param input
     * @param delimiter
     * @param n
     * @return output returns the n field of a line, given it's delimiter
     */
    public static String nthField(String input, String delimiter, int n) { //was nthSplit, did not return the last field. Bug or on purpose?
        ArrayList<String> array = fastSplitter(input, delimiter);
        if (n >= array.size()) {
            return "none";
        } else {
            return array.get(n);
        }
    }

    /**
     * @param input
     * @param ignored
     * @param n
     * @return output returns either the whole sequence without the periods or
     * the first n number of residues, without taking into account the periods
     */
    public static String getCleanSubSeq(String input, String ignored, int n) { //was fastIgnore()

        String tmp = input.replaceAll("\\.", "");

        if (n == -1) {
            return tmp;
        } else {
            return tmp.substring(0, Math.min(tmp.length(), n));
        }

    }

    /**
     * @param input
     * @return output returns the compliments of a input string replaces t->a
     * and a->t, c->g and g->c
     */
    public static String complimentaryOf(String input) {

        String output = new String();
        char[] inChars = input.toCharArray();

        int i;
        for (i = 0; i < inChars.length; i++) {
            char t = inChars[i];
            if (inChars[i] == 't') {
                inChars[i] = 'a';
            } else if (inChars[i] == 'c') {
                inChars[i] = 'g';
            } else if (inChars[i] == 'a') {
                inChars[i] = 't';
            } else if (inChars[i] == 'g') {
                inChars[i] = 'c';
            }
        }
        output = String.valueOf(inChars);
        return output;
    }

    /**
     * *************************************
     * main functions *****************************************
     */
    //TODO: does this function work?
//////    /**
//////     * @param folderIn
//////     * @param folderOut
//////     * @throws java.io.FileNotFoundException
//////     */
//////    public static void findCountsInFiles(String folderIn, String folderOut) throws FileNotFoundException, IOException {
//////        File inF = new File(folderIn);
//////        File out;
//////        File[] inFiles = inF.listFiles();
//////
//////        for (File inFile : inFiles) {
//////            out = new File(folderOut + inFile.getName());
//////            out.mkdirs();
//////            new BufferedWriter(new FileWriter(out));
//////        }
//////    }
    /**
     * @param outPath the path where the files generated from genomeAnalyze
     * reside
     * @param geneName the name of the "gene" that we will produce the final
     * count for
     * @ Description: Reports for all the files in the file path, which
     * positions have how many mutations. Makes more sense if u use it for a
     * gene at a time.
     * @throws java.io.FileNotFoundException
     */
    public static void findGeneralCounts(String inPath, String outPath, String geneName, boolean normalize) throws FileNotFoundException, IOException {
        File inFile = new File(inPath);
        File[] inFiles = inFile.listFiles();
        //this hashmap holds the mutation positions and how many times they have been found
        HashMap<String, Double> mutations = new HashMap<>();
        double fileCount = 0d;
        for (File f : inFiles) {
            if (!f.getName().contains("html") && !f.getName().equals("CountsAndAverage") && !f.getName().contains("final") && f.isFile() && f.getName().contains(geneName)) {
                fileCount++;
                BufferedReader file = new BufferedReader(new FileReader(f));
                String line;
                while ((line = file.readLine()) != null) {
                    String[] fields = line.split("\\t");
                    String mutationPosition = fields[3];
                    if (!mutations.containsKey(mutationPosition)) {
                        mutations.put(mutationPosition, 1d); //initialize
                    } else {
                        double count = mutations.get(mutationPosition) + 1d; // increase the number of times you have found this mutation position
                        mutations.put(mutationPosition, count);
                    }
                }
            }
        }

        if (fileCount != 0 && normalize) {
            for (String key : mutations.keySet()) {
                double value = mutations.get(key);
                value = value / fileCount; //normalize
                mutations.put(key, value);
            }
        }
        File finalFile;
        if (fileCount != 0 && normalize) {
            finalFile = new File(outPath + geneName + "_" + "final" + "_NormParam_" + fileCount + ".ARRAY");
        } else {
            finalFile = new File(outPath + geneName + "_" + "final" + ".ARRAY");
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(finalFile));
        for (Map.Entry<String, Double> mutationPosition : mutations.entrySet()) {
            writer.write(mutationPosition.getKey() + "\t" + mutationPosition.getValue());
            writer.newLine();
        }
        writer.close();
    }

    /**
     * @param fastqPath
     * @param file1
     * @param file2
     * @param outPath the file where the output will be stored finds the
     * mutations of each "gene" and outputs a report of what was read, what
     * should have been read, its position on the IMGT sequence, its position in
     * the fastq sequence, its quality and its orientation (forward or reverse)
     * @throws java.io.FileNotFoundException
     */
    public static void genomeAnalyze(String fastqPath, String file1, String file2, String outPath) throws FileNotFoundException, IOException {

        //hashmap that keeps a file for each gene and it's buffered writer
        HashMap<String, BufferedWriter> files = new HashMap<>();
        //hashmap that keeps the gene counts
        HashMap<String, HashMap<String, Integer[]>> geneCounts = new HashMap<>();
        //hashmap that keeps the genes in order to track duplicates
        HashMap<String, Integer> checkDuplicates = new HashMap<>(500000);

        BufferedReader fastqFile = new BufferedReader(new FileReader(new File(fastqPath))); //fastq file
        BufferedReader sevenFile = new BufferedReader(new FileReader(new File(file1))); // mutation location file
        BufferedReader twoFile = new BufferedReader(new FileReader(new File(file2))); //imgt sequence output file
        HashMap<String, String> countDoubles = new HashMap<>(10);

        // initialize parameters
        long t1 = System.currentTimeMillis();
        ArrayList<String> fieldsOfSevenFile;
        ArrayList<String> localMutations;
        ArrayList<String> quality; // quality array contains the qualities of each sequence residue
        int countIgnores = 0; // counts how many sequences had residues ignored by imgt
        String lineOfSevenFile;
        String localQuality = "null";
        String orientation = "ignore";
        int mutationLocationInFastq = 0;

        sevenFile.readLine(); // read the header
        twoFile.readLine(); // read the header

        while ((lineOfSevenFile = sevenFile.readLine()) != null) { // go through all the mutations for each fastq sequence
            fieldsOfSevenFile = fastSplitter(lineOfSevenFile, "\t"); // get the fields
            String lineOfTwoFile = twoFile.readLine(); // read the next line of the 2nd file. The 2 files are in sync this way

            String sequenceIdOfTwoFile = nthField(lineOfTwoFile, "\t", 1);
            sequenceIdOfTwoFile = sequenceIdOfTwoFile.substring(1, sequenceIdOfTwoFile.length());

            String vRegion = nthField(lineOfTwoFile, "\t", 6);

            if (fieldsOfSevenFile.size() > 4 && !vRegion.equals("")) {

                String gene = fieldsOfSevenFile.get(3); //V-Gene and allele
                gene = gene.replaceAll("Homsap ", "");
                gene = gene.replaceAll("\\*", "_"); //cannot use * in windows file naming

                //create the files for every gene
                if (!files.containsKey(gene)) { // if we havent already done this gene
                    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outPath + gene))); // make the file and the writer
                    HashMap<String, Integer[]> map = new HashMap<String, Integer[]>();
                    geneCounts.put(gene, map);
                    files.put(gene, writer);
                }

                String sequenceIdOfSevenFile = fieldsOfSevenFile.get(1).substring(1, fieldsOfSevenFile.get(1).length()); //??

                if (!sequenceIdOfSevenFile.equals(sequenceIdOfTwoFile)) {
                    System.out.println("mismatch between the two files sequence ID");
                    System.out.println(sequenceIdOfSevenFile);
                    System.out.println(sequenceIdOfTwoFile);
                    System.out.println();
                }
                String lineOfFastaqFile = fastqFile.readLine(); // read the next line of the fastq file. In sync with the other 2 files
                lineOfFastaqFile = lineOfFastaqFile.replaceAll(">", ""); // get the sequence id without the >>

                if (checkDuplicates.containsKey(sequenceIdOfSevenFile)) {
                    System.out.println("DOUBLE sequenceID contained");
                } else if (checkDuplicates.containsKey(lineOfFastaqFile)) {
                    System.out.println("DOUBLE fastq line contained");
                }

                //check for doubles in the id
                if (!sequenceIdOfSevenFile.equals(lineOfFastaqFile)) { // if all ids are in sync
                    String tmpLine2 = lineOfFastaqFile.substring(0, sequenceIdOfSevenFile.length());
                    if (checkDuplicates.containsKey(tmpLine2)) {
                        System.out.println("shortened FASTq ID is contained before");
                    }
                    if (!sequenceIdOfSevenFile.equals(tmpLine2)) {
                        System.out.println("totally equal");
                        System.out.println(sequenceIdOfSevenFile);
                        System.out.println(lineOfFastaqFile);

                    }
                }
                // add the sequence id in the duplicates map to keep its track
                checkDuplicates.put(sequenceIdOfSevenFile, 1);

                String fastqSequence = fastqFile.readLine(); //skip sequences
                lineOfFastaqFile = fastqFile.readLine(); //skip +
                lineOfFastaqFile = fastqFile.readLine(); //load quality array

                quality = fastArrayFormer(lineOfFastaqFile); //get the residue qualities

                //start transfer
                String vRegionNoSpaces = getCleanSubSeq(vRegion, ".", -1); // get the sequence without the dots

                ArrayList<String> newQuality = new ArrayList<String>(); // holds the qualities of a specific sequence

                if (fastqSequence.contains(vRegionNoSpaces.toUpperCase())) { // the imgt sequence is found in the fastq file
                    orientation = "forward"; // then imgt did not reverse it

                    //get the sequence from the fastq file
                    int start = fastqSequence.indexOf(vRegionNoSpaces.toUpperCase());
                    fastqSequence = fastqSequence.substring(start, start + vRegionNoSpaces.length());

                    for (int residue = start; residue < start + fastqSequence.length(); residue++) {
                        newQuality.add(quality.get(residue));
                    }

                    //TODO: you ever use newQuality    ?
                } else { //imgt changed the sequence. Investigate

                    vRegionNoSpaces = complimentaryOf(vRegionNoSpaces); // get the compliment sequence
                    // reverse it
                    vRegionNoSpaces = new StringBuffer(vRegionNoSpaces).reverse().toString();
                    orientation = "reverse";
                    if (!fastqSequence.contains(vRegionNoSpaces.toUpperCase())) {
                        orientation = "ignore";
                        countIgnores++; // the complimented and revered sequence was not found in the fastq file. IMGT ignored some residues
                    } else { // the reversed complimented sequence was found in fastq file
                        int start = fastqSequence.indexOf(vRegionNoSpaces.toUpperCase());
                        fastqSequence = fastqSequence.substring(start, start + vRegionNoSpaces.length());

                        for (int residue = start; residue < start + fastqSequence.length(); residue++) {
                            newQuality.add(quality.get(residue));
                        }
                    }
                }

                if (!fieldsOfSevenFile.get(4).equals("")) { //V-REGION
                    localMutations = fastSplitter(fieldsOfSevenFile.get(4), "\\|"); // get the mutations locations
                    for (String localV : localMutations) {
                        if (!countDoubles.containsKey(localV)) {
                            countDoubles.put(localV, "0"); // initialize
                            if (localV.contains(",")) {
                                // get the fist part e.g. from a128>g,Q43>R(+ - -) keep a128>g
                                localV = localV.split(",")[0]; //substring(0, localV.indexOf(",")); 
                            }
                            // keep the mutation location number e.g. 128
                            int mutationLocFromIMGT = Integer.valueOf(localV.substring(1, localV.indexOf(">")));
                            //get the first clean residues, until the mutation location
                            String mutationPartNoSpaces = getCleanSubSeq(vRegion, ".", mutationLocFromIMGT);
                            int mutationPosition = mutationPartNoSpaces.length() - 1;
                            String whatRead = localV.substring(0, 1); // the letter that was read
                            String whatShouldHaveRead = localV.substring(localV.indexOf(">") + 1, localV.indexOf(">") + 2); //the letters mutation

                            if (orientation.equals("forward")) {
                                localQuality = quality.get(mutationPosition); // get the quality
                                mutationLocationInFastq = mutationPosition;

                            } else if (orientation.equals("reverse")) {
                                //checking that reverse works
                                mutationLocationInFastq = fastqSequence.length() - mutationPosition;

                                localQuality = quality.get(mutationLocationInFastq);

                                whatRead = complimentaryOf(whatRead);
                                whatShouldHaveRead = complimentaryOf(whatShouldHaveRead);
                            }

                            if (!orientation.equals("ignore")) {

                                //keep the numbers in order to make an average report
                                String insert = whatRead + mutationLocFromIMGT + ">" + whatShouldHaveRead; //you have this already, you dont need to make it
                                HashMap<String, Integer[]> entryGene = geneCounts.get(gene);
                                Integer[] geneStats = new Integer[2];
                                if (entryGene.containsKey(insert)) {
                                    geneStats[0] = entryGene.get(insert)[0];
                                    geneStats[1] = entryGene.get(insert)[1];
                                    geneStats[0] = geneStats[0] + 1;
                                    geneStats[1] = geneStats[1] + Integer.valueOf(localQuality); //add quality

                                    entryGene.put(insert, geneStats);
                                } else { //initialize
                                    geneStats[0] = 1;
                                    geneStats[1] = Integer.valueOf(localQuality);
                                    entryGene.put(insert, geneStats);
                                }

                                files.get(gene).write(whatRead + "\t" + whatShouldHaveRead + "\t" + mutationLocFromIMGT + "\t" + mutationLocationInFastq + "\t" + localQuality + "\t" + orientation);
                                files.get(gene).newLine();
                            }
                        }
                    }
                }

                countDoubles.clear();

            } else {
                fastqFile.readLine();
                fastqFile.readLine();
                fastqFile.readLine();
                fastqFile.readLine();
            }
        }

        //make the averages report
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outPath + "CountsAndAverage")));
        for (Map.Entry<String, HashMap<String, Integer[]>> gene : geneCounts.entrySet()) {
            for (Map.Entry<String, Integer[]> geneStats : gene.getValue().entrySet()) {
                Integer[] statsTable = geneStats.getValue();
                String tmp = geneStats.getKey();
                if (tmp.contains(",")) {
                    tmp = tmp.split(",")[0]; //substring(0, tmp.indexOf(","));
                }
                int mutationPosition = Integer.valueOf(tmp.substring(1, tmp.indexOf(">")));
                String whatRead = tmp.substring(0, 1);
                String whatShouldHaveRead = tmp.substring(tmp.indexOf(">") + 1, tmp.indexOf(">") + 2);

                String s = gene.getKey() + "\t" + whatRead + "\t" + whatShouldHaveRead + "\t" + mutationPosition + "\t" + statsTable[1] / statsTable[0] + "\t" + statsTable[0];
                writer.write(s);
                writer.newLine();

            }
            files.get(gene.getKey()).close();
        }
        writer.close();
        long t2 = System.currentTimeMillis();

        System.out.println("time: " + (t2 - t1));
        System.out.println("ignored: " + countIgnores);
    }

    /**
     * @param inputFiles the file where the outputs of genomeAnalyze reside
     * @param outPath the file where the output will be stored.
     * @throws java.io.FileNotFoundException
     * @ Description: For all the files that genomeAnalyze has produced this
     * function calculates and reports for each mutation that appears, the
     * position it appeared in and how many times it appeared in this position
     * in all the files. It creates a separate file for each mutation
     */
    public static void analyzeMutationWise(String inputFiles, String outPath, String region) throws FileNotFoundException, IOException {
        File inFile = new File(inputFiles);
        File[] inFiles = inFile.listFiles();
        //hashmap that contains the mutations their positions and how many times they occur in each position
        HashMap<String, HashMap<String, Integer>> mutations = new HashMap<>();
        for (File inFile1 : inFiles) {
            if (!inFile1.getName().contains("html") && !inFile1.getName().equals("CountsAndAverage") && !inFile1.getName().contains("final") && inFile1.isFile() && inFile1.getName().contains(region)) {
                BufferedReader file = new BufferedReader(new FileReader(inFile1));
                String line;
                while ((line = file.readLine()) != null) {
                    if (line.length() >= 4) {
                        String mutation = line.substring(0, 3);
                        if (!mutations.containsKey(mutation)) {
                            //hashmap that contains the mutations positions and how many times they occur in each position 
                            HashMap<String, Integer> mutationInfo = new HashMap<>(1000); //initialize
                            mutations.put(mutation, mutationInfo);
                        }
                        String[] lineFields = line.split("\\t");
                        if (lineFields.length >= 4) {
                            if (!mutations.get(mutation).containsKey(lineFields[3])) { // for this mutation position (field 3)
                                mutations.get(mutation).put(lineFields[3], 1); //initialize
                            } else {
                                int count = mutations.get(mutation).get(lineFields[3]); // get this mutations name hashmap and from that hashmap get its value for key = the mutation position
                                count = count + 1;//increase the mutations value (how many times it has been found)
                                mutations.get(mutation).put(lineFields[3], count); //put it back
                            }
                        } else {
                            System.out.println(line);
                        }
                    } else {
                        System.out.println(line);
                    }
                }
            }
        }
        for (Map.Entry<String, HashMap<String, Integer>> mutation : mutations.entrySet()) {
            //   File mutationFile = new File(outPath + region + "_" + mutation.getKey() + ".ARRAY");
            String m = mutation.getKey().replace("\t", "");
            String mutationName = mutation.getKey().replace("\t", "");
            File mutationFile = new File(outPath + mutationName + ".ARRAY");
            FileWriter fileWriter = new FileWriter(mutationFile);
            try (BufferedWriter writer = new BufferedWriter(fileWriter)) {
                for (Map.Entry<String, Integer> mutationPosition : mutation.getValue().entrySet()) {
                    writer.write(mutationPosition.getKey() + "\t" + mutationPosition.getValue());
                    writer.newLine();
                }
            }
        }

    }

//    public static void main(String[] args) throws FileNotFoundException, IOException {
//        // TODO code application logic here
//
//        String fastqPath = "C:/Users/et3rn1ty/Documents/EKETAProject/ErrorCorrection/Data/stitched/T3241-18126144_proc__finaljoined_fastq.fastq";
//        String file1 = "C:/Users/et3rn1ty/Documents/EKETAProject/ErrorCorrection/Data/IMGT_output/7_V-REGION-mutation-and-AA-change-table_T3241_0_240415.txt";
//        String file2 = "C:/Users/et3rn1ty/Documents/EKETAProject/ErrorCorrection/Data/IMGT_output/2_IMGT-gapped-nt-sequences_T3241_0_240415.txt";
//        String outPath = "C:/Users/et3rn1ty/Documents/EKETAProject/ErrorCorrection/Data/testmutations/test";
//        File outDir = new File(outPath);
//        if(!outDir.exists()){
//            outDir.mkdir();
//        }else{
//            System.out.println("wont make it");
//        }
//        MutationMatcherGUI myGui = new MutationMatcherGUI();
//        myGui.paint(myGui);
//          //analyzeMutationWiseFromAllFiles("C:/Users/et3rn1ty/Documents/EKETAProject/ErrorCorrection/Data/testmutations/", "C:/Users/et3rn1ty/Documents/EKETAProject/ErrorCorrection/Data/testmutations/");
//        //    genomeAnalyze(fastqPath, file1, file2, outPath);
//      //  findGeneralCounts("C:/Users/et3rn1ty/Documents/EKETAProject/ErrorCorrection/Data/testmutations/", "TRBV2");
////////        BufferedReader r2 = new BufferedReader(new FileReader(new File(file1)));
////////        r2.readLine();
////////        String line = r2.readLine();
////////        ArrayList<String> mO = fastSplitter(line, "\t");
////////        ArrayList<String> mT = fastSplitterT(line, "\t");
////////        System.out.println("mut.line: " + line);
////////        for (String m : mO){
////////            System.out.println("o: " + m);
////////        }
////////        for (String m : mT){
////////            System.out.println("t: " + m);
////////        }
//        // analyzeMutationWiseFromAllFiles("/home/thanos/akraios/1/", "/home/thanos/akraios/mutWise/");
//        // findGeneralCounts(outPath);
//        //
//////        String line1 = "[[], [33, 38, 38, 38, 38, 37, 37, 38, 37, 37, 38, 38, 38, 38, 36, 38, 38, 37, 38, 38, 38, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 39, 39, 39, 39, 39, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 39, 38, 39, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 37, 37, 37, 38, 38, 36, 37, 38, 38, 38], []]";
//////        String line2 = "[[ 29, 16, 16, 29, 29, 33], [34, 37, 33, 33, 33, 37, 37, 37, 37, 37, 37, 36, 37, 38, 38, 38, 34, 39, 39, 39, 39, 39, 38, 38, 39, 39, 38, 38, 38, 38, 38, 38, 38, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 39, 39, 39, 39, 39, 39, 39, 39, 38, 38, 38, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 39, 39, 39, 39, 38, 39, 39, 38, 38, 39, 39, 38, 39, 39, 38, 39, 39, 39, 39, 39, 39, 38, 38, 39, 39, 38, 39, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 37, 37, 37, 37, 37, 37, 37], [33, 33, 33, 32, 88]]";
//////        ArrayList<String> thanos = fastSplitterT(line2,",");
//////        ArrayList<String> olga = fastSplitter(line2,",");
//////
//////        for (int i = 0; i < olga.size(); i++) {
//////            if (olga.get(i).isEmpty()) {
//////                System.out.println("AHAAAAAA");
//////            }
//////            System.out.println("o: " + olga.get(i) + " " + i);
//////            // System.out.println(thanos.get(i));
//////        }
//////        for (int i = 0; i < thanos.size(); i++) {
//////            if (thanos.get(i).isEmpty()) {
//////                System.out.println("AHAAAAAA");
//////            }
//////            System.out.println("t: " + thanos.get(i) + " " + i);
//////            // System.out.println(thanos.get(i));
//////        }
////        BufferedReader r3 = new BufferedReader(new FileReader(new File(file2)));
////        String line3 = r3.readLine();
////        line3 = r3.readLine();
////        String vRegion = nthField(line3, "\t", 6);
////        String vRegionNoSpaces = getCleanSubSeq(vRegion, ".", -1);
//
////        ArrayList<String> q0 = fastSplitter(line, "\\|");
////        ArrayList<String> qT = fastSplitterT(line, "\\|");
////        for (int i=0;i<qT.size();i++){
////            System.out.println("Thanos: " +qT.get(i));
////        }
////         for (int i=0;i<q0.size();i++){
////            System.out.println("Olga: " + q0.get(i));
////        }
////        BufferedReader r3 = new BufferedReader(new FileReader(new File(file2)));
////        String line3 = r3.readLine();
////        System.out.println(line3);
////        while ((line3 = r3.readLine()) != null) {
////            if (line3.contains("24_@M01691:35:000000000-A7BC4:1:1101:13676:2061")) {
////                System.out.println(line3);
////            }
////        }
//    }
}
