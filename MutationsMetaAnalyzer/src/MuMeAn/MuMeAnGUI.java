package MuMeAn;

import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.BarChartPlot;
import static com.googlecode.charts4j.Color.ALICEBLUE;
import static com.googlecode.charts4j.Color.BLACK;
import static com.googlecode.charts4j.Color.LAVENDER;
import static com.googlecode.charts4j.Color.WHITE;
import com.googlecode.charts4j.DataUtil;
import com.googlecode.charts4j.Fills;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.LineChart;
import com.googlecode.charts4j.LinearGradientFill;
import com.googlecode.charts4j.Plots;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import static java.lang.Math.pow;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UnsupportedLookAndFeelException;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

/**
 *
 * @author et3rn1ty
 */
public class MuMeAnGUI extends javax.swing.JPanel {

    /**
     * Creates new form MutationMatcherGUI
     */
    public MuMeAnGUI() {
        initComponents();
    }

    /**
     * @param message the message to be displayed in the file chooser
     * @param directory if true, allow the selection only of directories chooses
     * file
     */
    private File fileChooser(String message, boolean directory) {

        if (directory) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else {
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        }
        int returnVal = chooser.showDialog(this, message);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    /**
     * Checks to see if all the fields have been set and if yes then enables the
     * calculate buttons
     */
    private void enableButtons() {
        if (!outputTextField.getText().isEmpty() && option == 1) {
            onAnalyzeMutationWise.setEnabled(true);
            onAnalyzeRegion.setEnabled(true);
            onAnalyzeAllRegions.setEnabled(true);
            onAnalyzeMutationsPerRegion.setEnabled(true);
        } else if (!fastqTextField.getText().isEmpty() && !sevenFileTextField.getText().isEmpty() && !twoFileTextField.getText().isEmpty() && !outputTextField.getText().isEmpty()) {
            if (option == 0 || option == 10) { // user wants to override his folder or selected folder is empty
                onGenomeAnalyze.setEnabled(true);
            } else if (option == 1) { // the folder is not empty and the user doesn't want to override it
                onAnalyzeMutationWise.setEnabled(true);
                onAnalyzeRegion.setEnabled(true);
                onAnalyzeAllRegions.setEnabled(true);
                onAnalyzeMutationsPerRegion.setEnabled(true);
            }
        } else {
            onGenomeAnalyze.setEnabled(false);
        }
    }

    /**
     * @param hourglass if true, sets the cursor as running if false, sets the
     * cursor to normal
     */
    private void setCursorAsHourglass(boolean hourglass) {
        if (hourglass) {
            Cursor hourglassCursor = new Cursor(Cursor.WAIT_CURSOR);
            setCursor(hourglassCursor);
        } else {
            Cursor normalCursor = new Cursor(Cursor.DEFAULT_CURSOR);
            setCursor(normalCursor);
        }
    }

    private ArrayList<String> selectFiles(ArrayList<String> availableGenes) {

        ArrayList<String> selected = new ArrayList<>();
        ArrayList<JCheckBox> checkBoxes = new ArrayList<>();
        JPanel panel = new JPanel(new GridLayout(20, 3));
        JCheckBox checkBox = new JCheckBox("Select All");
        checkBoxes.add(checkBox);
        panel.add(checkBox);
        for (String gene : availableGenes) {
            checkBox = new JCheckBox(gene);
            checkBoxes.add(checkBox);
            panel.add(checkBox);
        }
        int option = JOptionPane.showConfirmDialog(this, panel, "Select", JOptionPane.OK_CANCEL_OPTION);
        if (option == 0) { // ok pressed
            for (JCheckBox button : checkBoxes) {
                if (button.isSelected()) {
                    if (button.getText().equals("Select All")) {
                        return availableGenes;
                    }
                    selected.add(button.getText()); // the genes selected
                }
            }
        }
        return selected;
    }

    private void plot(String filePath, String graphName, double yrange) throws IOException {

        setCursorAsHourglass(true);
        // what are we going to display
        ArrayList<String> files = new ArrayList<>();

        File inFile = new File(filePath);
        File[] inFiles = inFile.listFiles();

        for (File f : inFiles) {
            if (!f.getName().contains("html")) {
                files.add(f.getName());
            }
        }

        ArrayList<String> selectedFiles = selectFiles(files);
        if (selectedFiles.size() > 0) {
            ArrayList<String> endFiles = new ArrayList<>();
            ArrayList<String> filesInFiles = new ArrayList<>();

            for (String file : selectedFiles) {
                File f = new File(filePath + file);

                if (f.isDirectory()) { // loop and get its contents

                    File[] inDirectoryFiles = f.listFiles();

                    for (File inDirFile : inDirectoryFiles) {
                        filesInFiles.add(f.getName() + "/" + inDirFile.getName());
                    }
                } else {
                    endFiles.add(file);
                }
            }
            if (!filesInFiles.isEmpty()) {
                ArrayList<String> inDirSelected = selectFiles(filesInFiles);
                for (String selectedFile : inDirSelected) {
                    endFiles.add(selectedFile);
                }
            }

            //chosen files, make their graphs and html
            File htmlFile = new File(outPath + "\\" + graphName + ".html");
            BufferedWriter writer = new BufferedWriter(new FileWriter(htmlFile));
            writer.write("<html>");
            writer.newLine();
            writer.write("<body>");
            writer.newLine();
            writer.write("<table>");
            writer.newLine();
            writer.write("<tr>");
            writer.newLine();

            int rows = 0;
            for (String file : endFiles) {
                File selectedFile = new File(filePath + file);

                ArrayList<Double> x = new ArrayList<>();
                ArrayList<Double> y = new ArrayList<>();
                BufferedReader fileReader;
                try {
                    fileReader = new BufferedReader(new FileReader(selectedFile));
                    String line;
                    while ((line = fileReader.readLine()) != null) {
                        x.add(Double.valueOf(line.split("\\t")[0]));
                        y.add(Double.valueOf(line.split("\\t")[1]));
                    }
                } catch (FileNotFoundException ex) {
                    setCursorAsHourglass(false);
                    System.out.println("File Not Found");
                } catch (IOException ex) {
                    setCursorAsHourglass(false);
                    System.out.println("IO Exception");
                }

                //Make title more presentable
                String title = file.replace(".ARRAY","").replace("_final_"," ").replace("/"," ");
                URI uri = getGraphURI(x, y, yrange, title);

                if (rows < 3) {
                    writer.write("<td><img src=\"" + uri + "\"/></td>");
                    writer.newLine();
                    rows++;
                } else {
                    rows = 0;
                    writer.write("</tr>");
                    writer.newLine();
                    writer.write("<tr>");
                    writer.newLine();
                }

            }
            writer.close();
            Desktop.getDesktop().browse(htmlFile.toURI());

        }
        setCursorAsHourglass(false);
    }

    private URI getGraphURI(ArrayList<Double> x, ArrayList<Double> y, double yrange, String title) {

        URI uri = null;
        try {
            BarChartPlot sp = Plots.newBarChartPlot((DataUtil.scaleWithinRange(0, yrange, y)));

            LineChart chart = GCharts.newLineChart(sp);

            Collections.sort(x);
            Collections.sort(y);
            //turn x into string
            ArrayList<String> xs = new ArrayList<>();
            for (Double xi : x) {
                xs.add(xi.toString());
            }
            chart.addXAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, 370));
            chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, yrange));

            chart.setSize(500, 350);
            chart.setTitle(title, BLACK, 16);
            chart.setGrid(100, 10, 3, 2);
            chart.setBackgroundFill(Fills.newSolidFill(ALICEBLUE));
            LinearGradientFill fill = Fills.newLinearGradientFill(0, LAVENDER, 100);
            fill.addColorAndOffset(WHITE, 0);
            chart.setAreaFill(fill);

            String url = chart.toURLString();

            URL url1 = new URL(url);
            String nullFragment = null;
            uri = new URI(url1.getProtocol(), url1.getHost(), url1.getPath(), url1.getQuery(), nullFragment);
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException ex) {
            return null;
        }

        return uri;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        onFastqButton = new javax.swing.JButton();
        fastqTextField = new javax.swing.JTextField();
        onSevenFileButton = new javax.swing.JButton();
        sevenFileTextField = new javax.swing.JTextField();
        onTwoFileButton = new javax.swing.JButton();
        twoFileTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        outputTextField = new javax.swing.JTextField();
        onOutFolderButton = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        onGenomeAnalyze = new javax.swing.JButton();
        onAnalyzeMutationWise = new javax.swing.JButton();
        onAnalyzeRegion = new javax.swing.JButton();
        onAnalyzeAllRegions = new javax.swing.JButton();
        onAnalyzeMutationsPerRegion = new javax.swing.JButton();
        onAnalyzeMutWisePlot = new javax.swing.JButton();
        onAnalyzeRegPlot = new javax.swing.JButton();
        onAnalyzeAllRegPlot = new javax.swing.JButton();
        onAnalyzeMutPerRegionPlot = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        analyzeMutationsYRange = new javax.swing.JTextField();
        analyzeRegionsYRange = new javax.swing.JTextField();
        analyzeAllRegionsYRange = new javax.swing.JTextField();
        analyzeMutPerRegionYRange = new javax.swing.JTextField();

        jButton1.setText("jButton1");

        jLabel1.setText("Select fastq file");

        onFastqButton.setText("Browse");
        onFastqButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onFastqButtonActionPerformed(evt);
            }
        });

        fastqTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fastqTextFieldActionPerformed(evt);
            }
        });

        onSevenFileButton.setText("Browse");
        onSevenFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onSevenFileButtonActionPerformed(evt);
            }
        });

        onTwoFileButton.setText("Browse");
        onTwoFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onTwoFileButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("Select 7_X-REGION file");

        jLabel3.setText("Select 2_IMGT file");

        jLabel4.setText("Select results folder");

        onOutFolderButton.setText("Browse");
        onOutFolderButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onOutFolderButtonActionPerformed(evt);
            }
        });

        jLabel5.setText("Make the required selections for the calculations to take place");

        onGenomeAnalyze.setText("Genome Analyze");
        onGenomeAnalyze.setEnabled(false);
        onGenomeAnalyze.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onGenomeAnalyzeActionPerformed(evt);
            }
        });

        onAnalyzeMutationWise.setText("Analyze Mutations");
        onAnalyzeMutationWise.setEnabled(false);
        onAnalyzeMutationWise.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onAnalyzeMutationWiseActionPerformed(evt);
            }
        });

        onAnalyzeRegion.setText("Analyze Regions");
        onAnalyzeRegion.setEnabled(false);
        onAnalyzeRegion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onAnalyzeRegionActionPerformed(evt);
            }
        });

        onAnalyzeAllRegions.setText("Analyze All Regions");
        onAnalyzeAllRegions.setEnabled(false);
        onAnalyzeAllRegions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onAnalyzeAllRegionsActionPerformed(evt);
            }
        });

        onAnalyzeMutationsPerRegion.setText("Analyze Mutations Per Region");
        onAnalyzeMutationsPerRegion.setEnabled(false);
        onAnalyzeMutationsPerRegion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onAnalyzeMutationsPerRegionActionPerformed(evt);
            }
        });

        onAnalyzeMutWisePlot.setText("Plot It");
        onAnalyzeMutWisePlot.setEnabled(false);
        onAnalyzeMutWisePlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onAnalyzeMutWisePlotActionPerformed(evt);
            }
        });

        onAnalyzeRegPlot.setText("Plot It");
        onAnalyzeRegPlot.setEnabled(false);
        onAnalyzeRegPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onAnalyzeRegPlotActionPerformed(evt);
            }
        });

        onAnalyzeAllRegPlot.setText("Plot It");
        onAnalyzeAllRegPlot.setEnabled(false);
        onAnalyzeAllRegPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onAnalyzeAllRegPlotActionPerformed(evt);
            }
        });

        onAnalyzeMutPerRegionPlot.setText("Plot It");
        onAnalyzeMutPerRegionPlot.setEnabled(false);
        onAnalyzeMutPerRegionPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onAnalyzeMutPerRegionPlotActionPerformed(evt);
            }
        });

        jLabel6.setText("If you have the results ready, just select the folder in which they reside");

        jLabel7.setText("Select all if you want to enable the Genome Analyze functionality");

        analyzeMutationsYRange.setForeground(new java.awt.Color(102, 102, 102));
        analyzeMutationsYRange.setText("max y ");

        analyzeRegionsYRange.setForeground(new java.awt.Color(102, 102, 102));
        analyzeRegionsYRange.setText("max y ");

        analyzeAllRegionsYRange.setForeground(new java.awt.Color(102, 102, 102));
        analyzeAllRegionsYRange.setText("max y ");

        analyzeMutPerRegionYRange.setForeground(new java.awt.Color(102, 102, 102));
        analyzeMutPerRegionYRange.setText("max y ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(onAnalyzeMutationWise, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(onAnalyzeRegion, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(onAnalyzeMutWisePlot, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(analyzeMutationsYRange, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(onAnalyzeRegPlot, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(analyzeRegionsYRange, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(68, 68, 68)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(onAnalyzeMutationsPerRegion)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(onAnalyzeMutPerRegionPlot, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(analyzeMutPerRegionYRange, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(onAnalyzeAllRegions, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(onFastqButton)
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(onAnalyzeAllRegPlot, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(analyzeAllRegionsYRange, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addComponent(onSevenFileButton)
                                            .addComponent(onTwoFileButton)
                                            .addComponent(onOutFolderButton)))))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel4))
                                .addGap(40, 40, 40)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(outputTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(fastqTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE)
                                        .addComponent(sevenFileTextField)
                                        .addComponent(twoFileTextField))))))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 424, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 391, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 443, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(310, 310, 310)
                        .addComponent(onGenomeAnalyze, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7)
                .addGap(29, 29, 29)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(onFastqButton)
                    .addComponent(fastqTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(onSevenFileButton)
                    .addComponent(sevenFileTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(onTwoFileButton)
                    .addComponent(twoFileTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(14, 14, 14)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(outputTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(onOutFolderButton)
                    .addComponent(jLabel4))
                .addGap(24, 24, 24)
                .addComponent(onGenomeAnalyze)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(onAnalyzeMutationWise)
                    .addComponent(onAnalyzeAllRegions)
                    .addComponent(onAnalyzeAllRegPlot)
                    .addComponent(onAnalyzeMutWisePlot)
                    .addComponent(analyzeMutationsYRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(analyzeAllRegionsYRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(onAnalyzeRegion)
                    .addComponent(onAnalyzeRegPlot)
                    .addComponent(onAnalyzeMutationsPerRegion)
                    .addComponent(analyzeRegionsYRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(onAnalyzeMutPerRegionPlot)
                    .addComponent(analyzeMutPerRegionYRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(60, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void fastqTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fastqTextFieldActionPerformed

    }//GEN-LAST:event_fastqTextFieldActionPerformed

    private void onFastqButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onFastqButtonActionPerformed
        File file = fileChooser("Select fastq file", false);
        if (file != null) {
            fastqTextField.setText(file.getAbsolutePath());
            fastqPath = file.getAbsolutePath();
        }
        enableButtons();
    }//GEN-LAST:event_onFastqButtonActionPerformed

    private void onSevenFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onSevenFileButtonActionPerformed
        File file = fileChooser("Select 7_X-REGION file", false);
        if (file != null) {
            sevenFileTextField.setText(file.getAbsolutePath());
            file1 = file.getAbsolutePath();
        }
        enableButtons();
    }//GEN-LAST:event_onSevenFileButtonActionPerformed

    private void onTwoFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onTwoFileButtonActionPerformed
        File file = fileChooser("Select 2_IMGT file", false);
        if (file != null) {
            twoFileTextField.setText(file.getAbsolutePath());
            file2 = file.getAbsolutePath();
        }
        enableButtons();
    }//GEN-LAST:event_onTwoFileButtonActionPerformed

    private void onOutFolderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onOutFolderButtonActionPerformed
        File file = fileChooser("Select output folder", true);
        if (file != null) {
            outputTextField.setText(file.getAbsolutePath());
        }
        //make the required folders inside this file
        //they will be the output for the seperate calculations
        outPath = outputTextField.getText();
        File outDir = new File(outPath);
        if (!outDir.exists()) {
            outDir.mkdir();
        } else {
            File[] files = outDir.listFiles();
            //check if the folder is empty or not
            if (files.length > 0) {
                option = JOptionPane.showConfirmDialog(this, "The folder is not empty. Do you wish to calculate over it?", "Folder not empty!", JOptionPane.YES_NO_OPTION);
            }
        }
        enableButtons();
        outPath = outDir.getAbsolutePath();
        outPath = outPath + "\\";
    }//GEN-LAST:event_onOutFolderButtonActionPerformed

    private void onGenomeAnalyzeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onGenomeAnalyzeActionPerformed

        try {
            setCursorAsHourglass(true);
            MuMeAn.genomeAnalyze(fastqPath, file1, file2, outPath);
            setCursorAsHourglass(false);
            JOptionPane.showMessageDialog(this, "The calculations have completed", "Done", JOptionPane.INFORMATION_MESSAGE);
            onAnalyzeMutationWise.setEnabled(true);
            onAnalyzeRegion.setEnabled(true);
            onAnalyzeAllRegions.setEnabled(true);
            onAnalyzeMutationsPerRegion.setEnabled(true);
        } catch (IOException ex) {
            setCursorAsHourglass(false);
            Logger.getLogger(MuMeAnGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_onGenomeAnalyzeActionPerformed

    private void onAnalyzeMutationWiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onAnalyzeMutationWiseActionPerformed
        //mutations from ALL regions
        try {
            mutationWise = outPath + "MutationWise";
            File outDir = new File(mutationWise);
            if (!outDir.exists()) {
                outDir.mkdir();
            }
            mutationWise = outDir.getAbsolutePath();
            mutationWise = mutationWise + "\\";
            setCursorAsHourglass(true);
            MuMeAn.analyzeMutationWise(outPath, mutationWise, "TRB"); // mutations from ALL regions
            setCursorAsHourglass(false);
            JOptionPane.showMessageDialog(this, "Mutation wise data can be found at: " + mutationWise, "Done", JOptionPane.INFORMATION_MESSAGE);
            onAnalyzeMutWisePlot.setEnabled(true);
            analyzeMutationsYRange.setForeground(Color.BLACK);
        } catch (IOException ex) {
            setCursorAsHourglass(false);
            Logger.getLogger(MuMeAnGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_onAnalyzeMutationWiseActionPerformed

    private void onAnalyzeRegionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onAnalyzeRegionActionPerformed
        //this one needs normalization
        try {
            // get all the "genes"
            setCursorAsHourglass(true);
            ArrayList<String> genes = new ArrayList<>();

            File inFile = new File(outPath);
            File[] inFiles = inFile.listFiles();

            for (File f : inFiles) {
                if (!f.getName().contains("html") && !f.getName().equals("CountsAndAverage") && !f.getName().contains("final") && f.isFile()) {
                    String gene = f.getName();
                    if (gene.contains("*")) {
                        gene = gene.split("\\*")[0];
                    }
                    if (gene.contains(" ")) {
                        gene = gene.split(" ")[0];
                    }
                    if (!genes.contains(gene)) {
                        genes.add(gene);
                    }
                }
            }

            Collections.sort(genes);

            ArrayList<String> selectedGenes = selectFiles(genes);
            if (selectedGenes.size() > 0) {
                regionWise = outPath + "RegionWise";
                File outDir = new File(regionWise);
                if (!outDir.exists()) {
                    outDir.mkdir();
                }
                regionWise = outDir.getAbsolutePath();
                regionWise = regionWise + "\\";
                for (String gene : selectedGenes) {
                    MuMeAn.findGeneralCounts(outPath, regionWise, gene, true);
                }
                JOptionPane.showMessageDialog(this, "Region wise data can be found at: " + regionWise, "Done", JOptionPane.INFORMATION_MESSAGE);
                setCursorAsHourglass(false);
                onAnalyzeRegPlot.setEnabled(true);
                analyzeRegionsYRange.setForeground(Color.BLACK);
            } else {
                JOptionPane.showMessageDialog(this, "You need to select at least one region", "None selected", JOptionPane.INFORMATION_MESSAGE);
                setCursorAsHourglass(false);
            }
        } catch (IOException ex) {
            setCursorAsHourglass(false);
            Logger.getLogger(MuMeAnGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_onAnalyzeRegionActionPerformed

    private void onAnalyzeAllRegionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onAnalyzeAllRegionsActionPerformed
        try {
            setCursorAsHourglass(true);
            allRegions = outPath + "AllRegions";
            File outDir = new File(allRegions);
            if (!outDir.exists()) {
                outDir.mkdir();
            }
            allRegions = outDir.getAbsolutePath();
            allRegions = allRegions + "\\";
            MuMeAn.findGeneralCounts(outPath, allRegions, "TRB", false); // does this need to change, or will it always be TRB?
            JOptionPane.showMessageDialog(this, "Data for all regions can be found at: " + allRegions, "Done", JOptionPane.INFORMATION_MESSAGE);
            onAnalyzeAllRegPlot.setEnabled(true);
            analyzeAllRegionsYRange.setForeground(Color.BLACK);
            setCursorAsHourglass(false);

        } catch (IOException ex) {
            setCursorAsHourglass(false);
            Logger.getLogger(MuMeAnGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_onAnalyzeAllRegionsActionPerformed

    private void onAnalyzeMutationsPerRegionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onAnalyzeMutationsPerRegionActionPerformed
        try {
            // get all the "genes"
            setCursorAsHourglass(true);
            ArrayList<String> genes = new ArrayList<>();

            File inFile = new File(outPath);
            File[] inFiles = inFile.listFiles();

            for (File f : inFiles) {
                if (!f.getName().contains("html") && !f.getName().equals("CountsAndAverage") && !f.getName().contains("final") && f.isFile()) {
                    String gene = f.getName();
                    if (gene.contains("*")) {
                        gene = gene.split("\\*")[0];
                    }
                    if (gene.contains(" ")) {
                        gene = gene.split(" ")[0];
                    }
                    if (!genes.contains(gene)) {
                        genes.add(gene);
                    }
                }
            }

            Collections.sort(genes);

            ArrayList<String> selectedGenes = selectFiles(genes);
            if (selectedGenes.size() > 0) {
                String genePath = "";
                mutationsPerRegion = outPath + "MutationsPerRegion";
                File outDir = new File(mutationsPerRegion);
                if (!outDir.exists()) {
                    outDir.mkdir();
                }
                mutationsPerRegion = outDir.getAbsolutePath();
                mutationsPerRegion = mutationsPerRegion + "\\";

                for (String gene : selectedGenes) {
                    genePath = mutationsPerRegion + gene;
                    outDir = new File(genePath);
                    if (!outDir.exists()) {
                        outDir.mkdir();
                    }
                    genePath = outDir.getAbsolutePath();
                    genePath = genePath + "\\";
                    MuMeAn.analyzeMutationWise(outPath, genePath, gene);
                }
                JOptionPane.showMessageDialog(this, "Mutation wise per region data can be found at: " + mutationsPerRegion, "Done", JOptionPane.INFORMATION_MESSAGE);
                setCursorAsHourglass(false);
                onAnalyzeMutPerRegionPlot.setEnabled(true);
                analyzeMutPerRegionYRange.setForeground(Color.BLACK);
            } else {
                JOptionPane.showMessageDialog(this, "You need to select at least one region", "None selected", JOptionPane.INFORMATION_MESSAGE);
                setCursorAsHourglass(false);
            }
        } catch (IOException ex) {
            setCursorAsHourglass(false);
            Logger.getLogger(MuMeAnGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_onAnalyzeMutationsPerRegionActionPerformed

    private void onAnalyzeMutWisePlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onAnalyzeMutWisePlotActionPerformed
        Double yrange = null;
        if (!analyzeMutationsYRange.getText().isEmpty()) {
            try {
                yrange = Double.valueOf(analyzeMutationsYRange.getText());
            } catch (NumberFormatException ex) {
                yrange = null;
            }
        }
        try {
            if (yrange != null) {
                plot(mutationWise, "MutationsGraph", yrange);
            } else {
                plot(mutationWise, "MutationsGraph", (3.5 * pow(10, 4)));
            }
        } catch (IOException ex) {
            Logger.getLogger(MuMeAnGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_onAnalyzeMutWisePlotActionPerformed

    private void onAnalyzeRegPlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onAnalyzeRegPlotActionPerformed
        Double yrange = null;
        if (!analyzeRegionsYRange.getText().isEmpty()) {
            try {
                yrange = Double.valueOf(analyzeRegionsYRange.getText());
            } catch (NumberFormatException ex) {
                yrange = null;
            }
        }
        try {
            if (yrange != null) {
                plot(regionWise, "RegionsGraph", yrange);
            } else {
                plot(regionWise, "RegionsGraph", (2.5 * pow(10, 4)));
            }
        } catch (IOException ex) {
            Logger.getLogger(MuMeAnGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_onAnalyzeRegPlotActionPerformed

    private void onAnalyzeAllRegPlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onAnalyzeAllRegPlotActionPerformed
        Double yrange = null;
        if (!analyzeAllRegionsYRange.getText().isEmpty()) {
            try {
                yrange = Double.valueOf(analyzeAllRegionsYRange.getText());
            } catch (NumberFormatException ex) {
                yrange = null;
            }
        }
        try {
            if (yrange != null) {
                plot(allRegions, "AllRegionsGraph", yrange);
            } else {
                plot(allRegions, "AllRegionsGraph", 8.5 * pow(10, 4));
            }
        } catch (IOException ex) {
            Logger.getLogger(MuMeAnGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_onAnalyzeAllRegPlotActionPerformed

    private void onAnalyzeMutPerRegionPlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onAnalyzeMutPerRegionPlotActionPerformed
        Double yrange = null;
        if (!analyzeMutPerRegionYRange.getText().isEmpty()) {
            try {
                yrange = Double.valueOf(analyzeMutPerRegionYRange.getText());
            } catch (NumberFormatException ex) {
                yrange = null;
            }
        }
        try {
            if (yrange != null) {
                plot(mutationsPerRegion, "MutationsPerRegionGraph", yrange);
            } else {
                plot(mutationsPerRegion, "MutationsPerRegionGraph", (3.5 * pow(10, 4)));
            }
        } catch (IOException ex) {
            Logger.getLogger(MuMeAnGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_onAnalyzeMutPerRegionPlotActionPerformed

    public static void main(String[] args) {
        MuMeAnGUI myGui = new MuMeAnGUI();
        myGui.paint(myGui);
    }

    public void paint(MuMeAnGUI panel) {
        try {
            javax.swing.UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            JFrame.setDefaultLookAndFeelDecorated(true);
            JFrame frame = new JFrame("Mutations Meta-Analyser");

            frame.add(panel);
            panel.initComponents();
            frame.setIconImage(new ImageIcon(getClass().getResource("DNA-blue.jpg")).getImage());
            frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            //initialize chooser here in order to remember where it was last opened
            chooser = new JFileChooser();
            panel.setVisible(true);
            frame.setVisible(true);
            frame.pack();
            frame.setLocationRelativeTo(null);

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(MuMeAnGUI.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField analyzeAllRegionsYRange;
    private javax.swing.JTextField analyzeMutPerRegionYRange;
    private javax.swing.JTextField analyzeMutationsYRange;
    private javax.swing.JTextField analyzeRegionsYRange;
    private javax.swing.JTextField fastqTextField;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JButton onAnalyzeAllRegPlot;
    private javax.swing.JButton onAnalyzeAllRegions;
    private javax.swing.JButton onAnalyzeMutPerRegionPlot;
    private javax.swing.JButton onAnalyzeMutWisePlot;
    private javax.swing.JButton onAnalyzeMutationWise;
    private javax.swing.JButton onAnalyzeMutationsPerRegion;
    private javax.swing.JButton onAnalyzeRegPlot;
    private javax.swing.JButton onAnalyzeRegion;
    private javax.swing.JButton onFastqButton;
    private javax.swing.JButton onGenomeAnalyze;
    private javax.swing.JButton onOutFolderButton;
    private javax.swing.JButton onSevenFileButton;
    private javax.swing.JButton onTwoFileButton;
    private javax.swing.JTextField outputTextField;
    private javax.swing.JTextField sevenFileTextField;
    private javax.swing.JTextField twoFileTextField;
    // End of variables declaration//GEN-END:variables
    private String fastqPath;
    private String file1;
    private String file2;
    private String outPath;
    private JFileChooser chooser;
    private int option = 10;
    private String regionWise;
    private String mutationWise;
    private String allRegions;
    private String mutationsPerRegion;
}
