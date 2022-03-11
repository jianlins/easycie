/*
 * Copyright  2017  Department of Biomedical Informatics, University of Utah
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.utah.bmi.nlp.uima;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.internal.util.BrowserUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.tools.docanalyzer.PrefsMediator;
import org.apache.uima.tools.images.Images;
import org.apache.uima.tools.stylemap.ColorParser;
import org.apache.uima.tools.stylemap.StyleMapEditor;
import org.apache.uima.tools.stylemap.StyleMapEntry;
import org.apache.uima.tools.util.gui.Caption;
import org.apache.uima.tools.util.gui.SpringUtilities;
import org.apache.uima.tools.util.htmlview.AnnotationViewGenerator;
import org.apache.uima.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Dialog that loads analyzed documents stored in XMI or XCAS format and allows
 * them to be viewed using the Java-based CAS viewer or a web browser, in either
 * an HTML/Javascript format or in the inline XML format.
 */
public class MyAnnotationViewerPlain extends JFrame {

    private static final long serialVersionUID = -7259891069111863433L;

    private File tempDir = createTempDir();

    protected AnnotationViewGenerator annotationViewGenerator; // JMP

    private StyleMapEditor styleMapEditor;

    private PrefsMediator med1;

    private File styleMapFile;

    public String inputDirPath;

    JList analyzedResultsList;

    TypeSystem typeSystem = null;

    String[] typesToDisplay = null;

    JRadioButton javaViewerRB = null;

    JRadioButton javaViewerUCRB = null;

    JRadioButton htmlRB = null;

    JRadioButton xmlRB = null;

    private CAS cas;

    private boolean processedStyleMap = false;

    private String defaultCasViewName = CAS.NAME_DEFAULT_SOFA;

    private MyCasAnnotationViewer viewerPanel;

    public static void main(final String[] args) {
        JFrame frame = new MyAnnotationViewerPlain(args);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public MyAnnotationViewerPlain(String[] args) {
        super(args.length == 0 ? "AnnotationViewer" : args[0]);
        if (args.length == 0) {
            init("./", "desc/type/All_Types.xml", null);
        } else if (args.length == 3) {
            init(args[1], args[2], null);
        } else if (args.length > 3) {
            init(args[1], args[2], Arrays.copyOfRange(args, 3, args.length));
        }

    }


    public MyAnnotationViewerPlain(String aDialogTitle, String xmiDir, String descriptorFileString, final String[] aTypesToDisplay) {
        super(aDialogTitle);
        init(xmiDir, descriptorFileString, aTypesToDisplay);
    }

    // Create an AnnotationViewer Dialog

    public void init(String xmiDir, String descriptorFileString, final String[] aTypesToDisplay) {

        File descriptorFile = new File(descriptorFileString);

        try {
            Object descriptor = UIMAFramework.getXMLParser().parse(new XMLInputSource(descriptorFile));
            // instantiate CAS to get type system. Also build style map file if there is none.
            if (descriptor instanceof AnalysisEngineDescription) {
                cas = CasCreationUtils.createCas((AnalysisEngineDescription) descriptor);
                styleMapFile = getStyleMapFile((AnalysisEngineDescription) descriptor, descriptorFile
                        .getPath());
            } else if (descriptor instanceof TypeSystemDescription) {
                TypeSystemDescription tsDesc = (TypeSystemDescription) descriptor;
                tsDesc.resolveImports();
                cas = CasCreationUtils.createCas(tsDesc, null, new FsIndexDescription[0]);
                styleMapFile = getStyleMapFile((TypeSystemDescription) descriptor, descriptorFile.getPath());
            } else {
                displayError("Invalid Descriptor File \"" + descriptorFile.getPath() + "\""
                        + "Must be either an AnalysisEngine or TypeSystem descriptor.");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        typeSystem = cas.getTypeSystem();
        typesToDisplay = aTypesToDisplay;

        // create the AnnotationViewGenerator (for HTML view generation)
        annotationViewGenerator = new AnnotationViewGenerator(tempDir);

        // create StyleMapEditor dialog
        styleMapEditor = new StyleMapEditor(this, cas);


        JPanel resultsTitlePanel = new JPanel();
        resultsTitlePanel.setLayout(new BoxLayout(resultsTitlePanel, BoxLayout.Y_AXIS));

        resultsTitlePanel.add(new JLabel("These are the Analyzed Documents."));
        resultsTitlePanel.add(new JLabel("Select viewer type and double-click file to open."));

        // create an jlist to list the the analyzed documents

        File dir = new File(xmiDir);
        inputDirPath = dir.getAbsolutePath();
        // Select documents via filter. JMP
        FilenameFilter iFilter = new InteractiveFilter();
        String[] documents = dir.list(iFilter);
        if (documents==null)
            documents=new String[]{};
        Arrays.sort(documents);
        // create an empty array to display
        if (documents == null) {
            documents = new String[]{};
        }
        analyzedResultsList = new JList(documents);
        /*
         * File[] documents = dir.listFiles(); Vector docVector = new Vector();
		 * for (int i = 0; i < documents.length; i++) { if
		 * (documents[i].isFile()) { docVector.add(documents[i].getName()); } }
		 * final JList analyzedResultsList = new JList(docVector);
		 */
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(analyzedResultsList);

        JPanel southernPanel = new JPanel();
        southernPanel.setLayout(new BoxLayout(southernPanel, BoxLayout.Y_AXIS));

        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new SpringLayout());

        Caption displayFormatLabel = new Caption("Results Display Format:");
        controlsPanel.add(displayFormatLabel);

        JPanel displayFormatPanel = new JPanel();
        displayFormatPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        displayFormatPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        javaViewerRB = new JRadioButton("Java Viewer");
        javaViewerUCRB = new JRadioButton("JV user colors");
        htmlRB = new JRadioButton("HTML");
        xmlRB = new JRadioButton("XML");

        ButtonGroup displayFormatButtonGroup = new ButtonGroup();
        displayFormatButtonGroup.add(javaViewerRB);
        displayFormatButtonGroup.add(javaViewerUCRB);
        displayFormatButtonGroup.add(htmlRB);
        displayFormatButtonGroup.add(xmlRB);

        med1 = new PrefsMediator();
        // set OUTPUT dir in PrefsMediator, not input dir.
        // PrefsMediator is also used in DocumentAnalyzer, where the
        // output dir is the directory containing XCAS files.
        med1.setOutputDir(dir.toString());

        // select the appropraite viewer button according to user's prefs
        javaViewerRB.setSelected(true); // default, overriden below
        if ("Java Viewer".equals(med1.getViewType())) {
            javaViewerRB.setSelected(true);
        } else if ("JV User Colors".equals(med1.getViewType())) {
            javaViewerUCRB.setSelected(true);
        } else if ("HTML".equals(med1.getViewType())) {
            htmlRB.setSelected(true);
        } else if ("XML".equals(med1.getViewType())) {
            xmlRB.setSelected(true);
        }

        displayFormatPanel.add(javaViewerRB);
        displayFormatPanel.add(javaViewerUCRB);
        displayFormatPanel.add(htmlRB);
        displayFormatPanel.add(xmlRB);

        controlsPanel.add(displayFormatPanel);

        SpringUtilities.makeCompactGrid(controlsPanel, 1, 2, // rows, cols
                4, 4, // initX, initY
                0, 0); // xPad, yPad

        JButton editStyleMapButton = new JButton("Edit Style Map");

        // event for the editStyleMapButton button
//        editStyleMapButton.addActionListener(this);

//		southernPanel.add(controlsPanel);

        // southernPanel.add( new JSeparator() );

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        // APL: edit style map feature disabled for SDK
        buttonsPanel.add(editStyleMapButton);

        JButton closeButton = new JButton("Close");
        buttonsPanel.add(closeButton);

        southernPanel.add(buttonsPanel);

        JPanel listPanel = new JPanel();

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        contentPane.add(listPanel, BorderLayout.WEST);
        listPanel.setLayout(new BorderLayout(0, 0));
        analyzedResultsList = new JList(documents);
        listPanel.add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(analyzedResultsList);
        analyzedResultsList.setCellRenderer(new MyListCellRenderer());
        listPanel.add(southernPanel, BorderLayout.SOUTH);

        viewerPanel = new MyCasAnnotationViewer();
        contentPane.add(viewerPanel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, viewerPanel);

        contentPane.add(splitPane, BorderLayout.CENTER);
        contentPane.add(controlsPanel, BorderLayout.NORTH);


        // event for analyzedResultsDialog window closing
//        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLF(); // set default look and feel
        analyzedResultsList.setCellRenderer(new MyListCellRenderer());

        // doubleclicking on document shows the annotated result
        MouseListener mouseListener = new ListMouseAdapter();
        // styleMapFile, analyzedResultsList,
        // inputDirPath,typeSystem , typesToDisplay ,
        // javaViewerRB , javaViewerUCRB ,xmlRB ,
        // viewerDirectory , this);

        // add mouse Listener to the list
        analyzedResultsList.addMouseListener(mouseListener);
    }


    /**
     * @param tad
     * @param descFileName
     * @return the style map file
     * @throws IOException -
     */
    private File getStyleMapFile(AnalysisEngineDescription tad, String descFileName)
            throws IOException {
        File styleMapFile = getStyleMapFileName(descFileName);
        if (!styleMapFile.exists()) {
            // generate default style map
            String xml = AnnotationViewGenerator.autoGenerateStyleMap(tad.getAnalysisEngineMetaData());

            PrintWriter writer;
            writer = new PrintWriter(new BufferedWriter(new FileWriter(styleMapFile)));
            writer.println(xml);
            writer.close();
        }
        return styleMapFile;
    }


    private File getStyleMapFile(TypeSystemDescription tsd, String descFileName) throws IOException {
        File styleMapFile = getStyleMapFileName(descFileName);
        if (!styleMapFile.exists()) {
            // generate default style map
            String xml = AnnotationViewGenerator.autoGenerateStyleMap(tsd);

            PrintWriter writer;
            writer = new PrintWriter(new BufferedWriter(new FileWriter(styleMapFile)));
            writer.println(xml);
            writer.close();
        }
        return styleMapFile;
    }

    // Gets the name of the style map file for the given AE or TypeSystem descriptor filename.

    public File getStyleMapFileName(String aDescriptorFileName) {
        String baseName;
        int index = aDescriptorFileName.lastIndexOf(".");
        if (index > 0) {
            baseName = aDescriptorFileName.substring(0, index);
        } else {
            baseName = aDescriptorFileName;
        }
        return new File(baseName + "StyleMap.xml");
    }


    // unwound from small anonymous inner class
    public void actionPerformed(ActionEvent arg0) {
        // read style map XML file if it exists
        String styleMapXml = null;
        AnalysisEngineDescription selectedAE = null;
        try {
            if (styleMapFile.exists()) {
                styleMapXml = FileUtils.file2String(styleMapFile);
            }

            // have user select AE if they haven't done so already
            if (selectedAE == null) {
                selectedAE = promptForAE();
            }
            if (selectedAE != null) {
                styleMapEditor.setAnalysisEngine(selectedAE);
                // launch StyleMapEditor GUI
                String newStyleMap = styleMapEditor.launchEditor(selectedAE.getAnalysisEngineMetaData(), styleMapXml,
                        cas);

                if (newStyleMap != null) {
                    // write new file using AE+StyleMap convention
                    styleMapFile = med1.getStylemapFile();
                    // write new style map to disk
                    FileWriter writer = new FileWriter(styleMapFile);
                    writer.write(newStyleMap);
                    writer.close();
                    // process generated style map
                    annotationViewGenerator.processStyleMap(styleMapFile);
                }
            }
        } catch (Exception e) {
            displayError(e);
        }
    }

    /**
     * Filter to not show the two interactive-mode directories in the file list
     */
    static class InteractiveFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            if (name.equals("interactive_temp"))
                return false;
            return !name.equals("interactive_out");
        }
    }

    // Gets the name of the CAS View that will be displayed first in the annotation viewer.

    public String getDefaultCasViewName() {
        return defaultCasViewName;
    }

    // Sets the name of the CAS View that will be displayed first in the annotation viewer.
    // It not set, defaults to {@link CAS#NAME_DEFAULT_SOFA}.

    public void setDefaultCasViewName(String defaultCasViewName) {
        this.defaultCasViewName = defaultCasViewName;
    }

    // Common code to launch viewer for both the Run (file-based) and
    // Interactive modes
    // JMP

    public void launchThatViewer(String inputDirPath, String fileName, TypeSystem typeSystem,
                                 final String[] aTypesToDisplay, boolean javaViewerRBisSelected, boolean javaViewerUCRBisSelected,
                                 boolean xmlRBisSelected, File styleMapFile, File viewerDirectory) {
        try {

            File xcasFile = new File(inputDirPath, fileName);
            // create a new CAS
            CAS cas = CasCreationUtils.createCas(Collections.EMPTY_LIST, typeSystem,
                    UIMAFramework.getDefaultPerformanceTuningProperties());
            // deserialize XCAS into CAS
            FileInputStream xcasInStream = null;
            try {
                xcasInStream = new FileInputStream(xcasFile);
                XmlCasDeserializer.deserialize(xcasInStream, cas, true);
            } finally {
                if (xcasInStream != null)
                    xcasInStream.close();
            }

            // get the specified view
            cas = cas.getView(this.defaultCasViewName);

            // launch appropriate viewer
            if (javaViewerRBisSelected || javaViewerUCRBisSelected) { // JMP
                // record preference for next time
                med1.setViewType(javaViewerRBisSelected ? "Java Viewer" : "JV User Colors");
                // TODO add panel to this dialog
                // create tree viewer component
                viewerPanel.setDisplayedTypes(aTypesToDisplay);
                if (javaViewerUCRBisSelected)
                    getColorsForTypesFromFile(viewerPanel, styleMapFile);
                else {
                    viewerPanel.setHiddenTypes(new String[]{"uima.cpm.FileLocation"});
                }
                // launch viewer in a new dialog
                viewerPanel.setCAS(cas);

            } else {
                CAS defaultView = cas.getView(CAS.NAME_DEFAULT_SOFA);
                if (defaultView.getDocumentText() == null) {
                    displayError(
                            "The HTML and XML Viewers can only view the default text document, which was not found in this CAS.");
                    return;
                }
                // generate inline XML
                File inlineXmlFile = new File(viewerDirectory, "inline.xml");
                CasToInlineXml casToInlineXml = new CasToInlineXml();
                casToInlineXml.setFormattedOutput(false);
                String xmlAnnotations = casToInlineXml.generateXML(defaultView);
                FileOutputStream outStream = new FileOutputStream(inlineXmlFile);
                outStream.write(xmlAnnotations.getBytes("UTF-8"));
                outStream.close();

                if (xmlRBisSelected) // JMP passed in
                {
                    // record preference for next time
                    med1.setViewType("XML");

                    BrowserUtil.openUrlInDefaultBrowser(inlineXmlFile.getAbsolutePath());
                } else
                // HTML view
                {
                    med1.setViewType("HTML");
                    // generate HTML view
                    // first process style map if not done already
                    if (!processedStyleMap) {
                        if (!styleMapFile.exists()) {
                            annotationViewGenerator.autoGenerateStyleMapFile(promptForAE().getAnalysisEngineMetaData(),
                                    styleMapFile);
                        }
                        annotationViewGenerator.processStyleMap(styleMapFile);
                        processedStyleMap = true;
                    }
                    annotationViewGenerator.processDocument(inlineXmlFile);
                    File genFile = new File(viewerDirectory, "index.html");
                    // open in browser
                    BrowserUtil.openUrlInDefaultBrowser(genFile.getAbsolutePath());
                }
            }

            // end LTV here

        } catch (Exception ex) {
            displayError(ex);
        }
    }

    // Assumes node has a text field and extracts its value. JMP

    static public String getTextValue(Node node) {
        Node first = node.getFirstChild();
        if (first != null) {
            Text text = (Text) node.getFirstChild();
            return text.getNodeValue().trim();
        } else
            return null;
    }

    // Gets the first child with a given name. JMP

    static public Node getFirstChildByName(Node node, String name) {
        NodeList children = node.getChildNodes();
        for (int c = 0; c < children.getLength(); ++c) {
            Node n = children.item(c);
            if (n.getNodeName().equals(name))
                return n;
        }
        return null;
    }


    public void getColorsForTypesFromFile(MyCasAnnotationViewer viewer, File aStyleMapFile) {
        List<Color> colorList = new ArrayList<Color>();
        List<String> typeList = new ArrayList<String>();
        List<String> notCheckedList = new ArrayList<String>();
        ArrayList hiddenList = new ArrayList();
        hiddenList.add("uima.cpm.FileLocation");

        if (aStyleMapFile.exists()) {

            FileInputStream stream = null;
            Document parse = null;
            try {
                stream = new FileInputStream(aStyleMapFile);
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                parse = db.parse(stream);
            } catch (FileNotFoundException e) {
                throw new UIMARuntimeException(e);
            } catch (ParserConfigurationException e) {
                throw new UIMARuntimeException(e);
            } catch (FactoryConfigurationError e) {
                throw new UIMARuntimeException(e);
            } catch (SAXException e) {
                throw new UIMARuntimeException(e);
            } catch (IOException e) {
                throw new UIMARuntimeException(e);
            }
            Node node0 = parse.getDocumentElement();
            // Node node1 = getFirstChildByName(parse.getDocumentElement(),
            // "styleMap");
            // String node1Name = node1.getNodeName();

            NodeList nodeList = node0.getChildNodes();
            ColorParser cParser = new ColorParser();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                String nodeName = node.getNodeName();
                if (nodeName.equals("rule")) {
                    NodeList childrenList = node.getChildNodes();
                    String type = "";
                    String label = "";
                    StyleMapEntry sme = null;
                    String colorText = "";
                    for (int j = 0; j < childrenList.getLength(); j++) {
                        Node child = childrenList.item(j);
                        String childName = child.getNodeName();
                        if (childName.equals("pattern")) {
                            type = getTextValue(child);
                        }
                        if (childName.equals("label")) {
                            label = getTextValue(child);
                        }
                        if (childName.equals("style")) {
                            colorText = getTextValue(child);
                        }

                    }
                    sme = cParser.parseAndAssignColors(type, label, label, colorText);
                    if (!sme.getChecked()) {
                        notCheckedList.add(sme.getAnnotationTypeName());
                    }
                    if (!sme.getHidden()) {
                        colorList.add(sme.getBackground());
                        typeList.add(sme.getAnnotationTypeName());
                    } else {
                        hiddenList.add(sme.getAnnotationTypeName());
                    }

                }
            }

            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            viewer.assignColorsFromList(colorList, typeList);
            viewer.assignCheckedFromList(notCheckedList);
            String[] hiddenArr = new String[hiddenList.size()];
            hiddenList.toArray(hiddenArr);
            viewer.setHiddenTypes(hiddenArr);
        }

    }


    public void displayError(String aErrorString) {
        // word-wrap long mesages
        StringBuffer buf = new StringBuffer(aErrorString.length());
        final int CHARS_PER_LINE = 80;
        int charCount = 0;
        StringTokenizer tokenizer = new StringTokenizer(aErrorString, " \n", true);

        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken();

            if (tok.equals("\n")) {
                buf.append("\n");
                charCount = 0;
            } else if ((charCount > 0) && ((charCount + tok.length()) > CHARS_PER_LINE)) {
                buf.append("\n").append(tok);
                charCount = tok.length();
            } else {
                buf.append(tok);
                charCount += tok.length();
            }
        }

        JOptionPane.showMessageDialog(MyAnnotationViewerPlain.this, buf.toString(), "Error",
                JOptionPane.ERROR_MESSAGE);
    }


    public void displayError(Throwable aThrowable) {
        aThrowable.printStackTrace();

        String message = aThrowable.toString();

        // For UIMAExceptions or UIMARuntimeExceptions, add cause info.
        // We have to go through this nonsense to support Java 1.3.
        // In 1.4 all exceptions can have a cause, so this wouldn't involve
        // all of this typecasting.
        while ((aThrowable instanceof UIMAException) || (aThrowable instanceof UIMARuntimeException)) {
            if (aThrowable instanceof UIMAException) {
                aThrowable = aThrowable.getCause();
            } else if (aThrowable instanceof UIMARuntimeException) {
                aThrowable = aThrowable.getCause();
            }

            if (aThrowable != null) {
                message += ("\nCausedBy: " + aThrowable.toString());
            }
        }

        displayError(message);
    }

    // If the current AE filename is not know ask for it. Then parse the   selected file and return the AnalysisEngineDescription object.

    protected AnalysisEngineDescription promptForAE()
            throws IOException, InvalidXMLException, ResourceInitializationException {
        if (med1.getTAEfile() != null) {
            File taeFile = new File(med1.getTAEfile());
            XMLInputSource in = new XMLInputSource(taeFile);
            AnalysisEngineDescription aed = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
            return aed;
        } else {
            String taeDir = med1.getTAEfile();
            JFileChooser chooser = new JFileChooser(taeDir);
            chooser.setDialogTitle("Select the Analysis Engine that Generated this Output");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                XMLInputSource in = new XMLInputSource(chooser.getSelectedFile());
                return UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
            } else {
                return null;
            }
        }
    }

    // set default look and feel

    private static void setLF() {
        // Force SwingApp to come up in the System L&F
        String laf = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(laf);
        } catch (UnsupportedLookAndFeelException exc) {
            System.err.println("Warning: UnsupportedLookAndFeel: " + laf);
        } catch (Exception exc) {
            System.err.println("Error loading " + laf + ": " + exc);
        }
    }

    private File createTempDir() {
        File temp = new File(System.getProperty("java.io.tmpdir"), System.getProperty("user.name"));
        temp.mkdir();
        return temp;
    }

    // create and call the list cell renderer to set the selected color and
    // image icon
    static class MyListCellRenderer extends JLabel implements ListCellRenderer {
        private static final long serialVersionUID = 7231915634689270693L;

        public MyListCellRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(JList analyzedResultsList, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            ImageIcon xmlIcon = Images.getImageIcon(Images.XML_DOC);
            setIcon(xmlIcon);
            setText(value.toString());
            setBackground(isSelected ? Color.orange : Color.white);
            setForeground(isSelected ? Color.black : Color.black);
            return this;
        }
    }

    class ListMouseAdapter extends MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            try {
                if (e.getClickCount() == 1) {
                    String fileName = ((String) analyzedResultsList.getSelectedValue());
                    if (fileName != null) {
                        analyzedResultsList.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        // Start LTV here
                        launchThatViewer(inputDirPath, fileName, typeSystem, typesToDisplay, javaViewerRB.isSelected(),
                                javaViewerUCRB.isSelected(), xmlRB.isSelected(), styleMapFile, tempDir);

                        analyzedResultsList.setCursor(Cursor.getDefaultCursor());
                    }
                }
            } catch (Exception ex) {
                displayError(ex);
            }
        }
    }
}
