/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iphonestalker.gui;

import iphonestalker.data.IPhoneData;
import iphonestalker.data.IPhoneData.IPhoneLocation;
import iphonestalker.data.MyCoordinate;
import iphonestalker.gui.interfaces.MapRoute;
import iphonestalker.util.BackupReader;
import iphonestalker.util.io.InfoReader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

/**
 *
 * @author MikeUCFL
 */
public class MainGUI extends JFrame implements ActionListener {

    private static final Logger logger =
            Logger.getLogger(MainGUI.class.getName());
    private static final String ADD = "Add backup folder";
    private static final String LOAD = "Load";
    private static final String SWITCH_MAP_TILE = "Switch map tile";
    private static final String EXPORT = "Export";
    private static final String EXIT = "Exit";
    private static final String FILE_SEPARATOR =
            System.getProperty("file.separator");
    private static final String FOLDER_PATH = "MobileSync" + FILE_SEPARATOR
            + "Backup" + FILE_SEPARATOR;
    private boolean initialized = false;
    private JMenuBar menuBar = null;
    private TrackerTableModel tableModel = null;
    private TrackerTableModel sorterModel = null;
    private JComboBox phoneSelector = null;
    private JTable table = null;
    private BackupReader backupReader = null;
    private InfoReader infoReader = null;
    private MyJMapViewer mapViewer = null;
    private JComboBox tileSourceSelector = null;
    private JTextField statusBar = null;
    private String statusLog = "";

    public MainGUI() {
        super("iPhoneStalker");
    }

    public boolean initialize() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane
        setPreferredSize(new Dimension(1200, 600));
        setJMenuBar(createMenuBar());
        setContentPane(createContentPane());

        backupReader = new BackupReader();
        infoReader = new InfoReader();
        String systemOs = System.getProperty("os.name");

        // System specific areas
        String dataPath = null;
        String laf = UIManager.getCrossPlatformLookAndFeelClassName();
        if (systemOs.startsWith("Windows")) {
            dataPath = System.getenv("APPDATA") + FILE_SEPARATOR
                    + "Apple Computer" + FILE_SEPARATOR + FOLDER_PATH;
            laf = UIManager.getSystemLookAndFeelClassName();
        } else if (systemOs.startsWith("Mac OS")) {
            laf = UIManager.getSystemLookAndFeelClassName();
            dataPath = System.getProperty("user.home") + ""
                    + "/Library/Application Support/" + FOLDER_PATH;
        } else {
            logger.log(Level.INFO, "{0} is not supported! Please manually select"
                    + " a backup folder.", systemOs);
        }

        if (laf != null) {
            try {
                UIManager.setLookAndFeel(laf);
                SwingUtilities.updateComponentTreeUI(this);
            } catch (Exception ex) {
                Logger.getLogger(MainGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        final String dataPathFinal = dataPath;
        if (dataPathFinal != null) {
            // Get the list of backup directories
            SwingWorker worker = new SwingWorker<Void, Void>() {

                @Override
                protected Void doInBackground() throws Exception {
                    addStatus("Loading phones in '" + dataPathFinal + "'");
                    File dir = new File(dataPathFinal);

                    String[] children = dir.list();
                    if (children == null) {
                        // Either dir does not exist or is not a directory
                    } else {
                        for (int i = 0; i < children.length; i++) {
                            // Get filename of file or directory
                            String filename = children[i];

                            if (!filename.contains("-")) {
                                String backupFolder = dataPathFinal + FILE_SEPARATOR
                                        + filename + FILE_SEPARATOR;

                                addBackupFolder(backupFolder, false);
                            }
                        }
                    }
                    return null;
                }
            };

            worker.execute();
        }

        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);

        initialized = true;
        return initialized;
    }

    private JMenuBar createMenuBar() {
        JMenu menu;
        JMenuItem menuItem;

        menuBar = new JMenuBar();

        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menu);

        menuItem = new JMenuItem(ADD, KeyEvent.VK_A);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_A, ActionEvent.ALT_MASK));
        menuItem.setActionCommand(ADD);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem(EXPORT, KeyEvent.VK_E);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_E, ActionEvent.ALT_MASK));
        menuItem.setActionCommand(EXPORT);
        menuItem.addActionListener(this);
        menu.add(menuItem);
        menu.addSeparator();

        menuItem = new JMenuItem(EXIT, KeyEvent.VK_X);
        menuItem.setActionCommand(EXIT);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        return menuBar;
    }

    private JToolBar createToolBar() {

        JPanel toolBarPanel = new JPanel();
        toolBarPanel.setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();

        JButton loadButton = createToolBarButton(ADD, "icons/Add16.gif");
        toolBar.add(loadButton);

        JButton saveAllButton = createToolBarButton(EXPORT, "icons/SaveAll16.gif");
        toolBar.add(saveAllButton);

        return toolBar;
    }

    private JButton createToolBarButton(String text, String icon) {

        JButton button = new JButton();
        button.setToolTipText(text);
        if (icon != null) {
            URL imgURL = getClass().getResource(icon);
            if (imgURL != null) {
                button.setIcon(new ImageIcon(imgURL, text));
            } else {
                button.setText(text);
            }
        } else {
            button.setText(text);
        }

        button.addActionListener(this);
        button.setActionCommand(text);

        return button;

    }

    private Container createContentPane() {
        JPanel contentPane = new JPanel(new BorderLayout());
        JPanel centerPane = new JPanel(new GridBagLayout());
        JPanel centerLeftPane = new JPanel(new GridBagLayout());
        JPanel centerRightPane = new JPanel(new GridBagLayout());
        JPanel bottomPane = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.BOTH;
        JToolBar toolBar = createToolBar();
        contentPane.add(toolBar, BorderLayout.NORTH);

        // Add the phone selector pane
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 50, 0, 50);
        phoneSelector = new JComboBox();
        phoneSelector.setActionCommand(LOAD);
        phoneSelector.addActionListener(this);
        centerLeftPane.add(phoneSelector, c);

        // Create the table model
        tableModel = new TrackerTableModel();
        table = new JTable(tableModel) {

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component component = super.prepareRenderer(renderer, row, col);

                if (!isCellSelected(row, col)) {
                    if (row % 2 == 0) {
                        component.setBackground(new Color(245, 245, 245)); // light grey
                    } else {
                        component.setBackground(Color.white);
                    }
                }
                return component;
            }
        };
        
        // Create the table sorter
        TableRowSorter<TrackerTableModel> sorter =
                new TableRowSorter<TrackerTableModel>(tableModel);
        
        // Comparator for sorting the first "index" column
        sorter.setComparator(0, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        table.setRowSorter(sorter);
        sorterModel = sorter.getModel();
        
        // Adjust the column of the table
        TableColumn col = table.getColumnModel().getColumn(0);
        col.setPreferredWidth(1); // squeezes the other columns
        
        // Set attributes for the table
        table.setSelectionBackground(new Color(255, 250, 205)); // light yellow
        table.setSelectionForeground(Color.BLACK);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int[] selectedRows = table.getSelectedRows();
                    int[] sortedRows = null;
                    
                    if (selectedRows != null) {
                        sortedRows = new int[selectedRows.length];
                        for (int i = 0; i < selectedRows.length;i++) {
                            sortedRows[i] = table.convertRowIndexToModel(selectedRows[i]);
                        }
                    }
                    
                    IPhoneData iPhoneData = (IPhoneData) phoneSelector.getSelectedItem();

                    ArrayList<MapRoute> mapRouteList = new ArrayList<MapRoute>();
                    if (sortedRows != null) {
                        for (int row : sortedRows) {

                            String day = tableModel.getDay(row);
                            ArrayList<IPhoneLocation> iPhoneLocations =
                                    iPhoneData.getIphoneLocations(day);

                            // Create the map route
                            ArrayList<MyCoordinate> mapCoordinateList =
                                    new ArrayList<MyCoordinate>();

                            for (int i = 0; i < iPhoneLocations.size(); i++) {
                                IPhoneLocation iPhoneLocation = iPhoneLocations.get(i);

                                String label = iPhoneLocation.getFullDate() +
                                        " [route: " + (i + 1) + "/" + iPhoneLocations.size()
                                        + ", confidence: " + iPhoneLocation.confidence
                                        + "]";
                                MyCoordinate coordinate = 
                                        new MyCoordinate(label,iPhoneLocation);
                                mapCoordinateList.add(coordinate);
                            }

                            MyMapRoute mapRoute =
                                    new MyMapRoute(
                                    null,
                                    Color.BLUE,
                                    mapCoordinateList);
                            mapRouteList.add(mapRoute);
                        }
                    }

                    mapViewer.setMapRouteList(mapRouteList);

                    if (!mapRouteList.isEmpty()) {
                        mapViewer.setDisplayToFitMapRoutes();
                    }
                }
            }
        });

        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 5, 5, 5);
        centerLeftPane.add(new JScrollPane(table), c);

        c.gridx = 0;
        c.gridy = 0;
        //c.gridheight = 2;
        //c.gridwidth = 2;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        mapViewer = MyJMapViewer.getInstance();
        centerRightPane.add(new JLabel(SWITCH_MAP_TILE), c);
        tileSourceSelector = new JComboBox(new TileSource[]{
                    new OsmTileSource.Mapnik(), new OsmTileSource.TilesAtHome(),
                    new OsmTileSource.CycleMap(), new BingAerialTileSource()
                });
        tileSourceSelector.setActionCommand(SWITCH_MAP_TILE);
        tileSourceSelector.addActionListener(this);
        c.weightx = 1.0;
        c.gridx = 1;
        centerRightPane.add(tileSourceSelector, c);

        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        centerRightPane.add(mapViewer, c);
        c.gridy = 2;
        c.weighty = 0.0;
        centerRightPane.add(new JLabel("Use the right mouse button to move. "
                + " Double click left mouse button or use mouse wheel to zoom.",
                SwingConstants.CENTER), c);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                centerLeftPane, centerRightPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation((int) (getPreferredSize().width / 2.5));

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        centerPane.add(splitPane, c);
        contentPane.add(centerPane, BorderLayout.CENTER);

        statusBar = new JTextField("");
        statusBar.setEditable(false);
        statusBar.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                JTextArea statusLogTextArea = new JTextArea(statusLog);
                statusLogTextArea.setEditable(false);
                JScrollPane scrollPane = new JScrollPane(statusLogTextArea);
                
                JOptionPane.showMessageDialog(null, scrollPane, 
                        "Log", JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
            
        });
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.BOTH;
        bottomPane.add(statusBar, c);
        
        contentPane.add(bottomPane, BorderLayout.SOUTH);

        return contentPane;
    }

    private void populateTable(IPhoneData iPhoneData) {
        tableModel.setData(iPhoneData);
    }

    private void addBackupFolder(String backupFolder, boolean alertInfo) {
        File infoFile = new File(backupFolder + FILE_SEPARATOR + "Info.plist");
        if (infoFile.exists()) {
            IPhoneData iPhoneData =
                    infoReader.parseFile(infoFile);

            // Get the data from the file
            String errorReason = backupReader.processFolder(backupFolder, iPhoneData);

            // Check for the override flag
            if (errorReason != null && errorReason.startsWith("OVERRIDE")) {
                errorReason = errorReason.replace("OVERRIDE", "");
                alertInfo = true;
            }
            
            // Add to list
            if (errorReason != null && alertInfo) {
                addStatus("ERROR: " + errorReason);
            } else {
                phoneSelector.addItem(iPhoneData);
                File backupFolderFile = new File(backupFolder);
                addStatus("Loaded '" + iPhoneData + "' [" + 
                        backupFolderFile.getName() + "]");
            }
        } else if (alertInfo) {
            addStatus("ERROR: Unable to load " + infoFile.getAbsolutePath());
        }
    }

    public void addStatus(String status) {
        statusBar.setText(status);
        statusLog += "\n" + status;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(ADD)) {
            JFileChooser fc = new JFileChooser("Browse for the backup directory"
                    + " process");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                if (file != null) {
                    addBackupFolder(file.toString(), true);
                }
            }
        } else if (e.getActionCommand().equals(EXPORT)) {
            int[] selectedRows = table.getSelectedRows();

            ArrayList<String> days = new ArrayList<String>();
            if (selectedRows != null) {
                for (int row : selectedRows) {
                    days.add(tableModel.getDay(row));
                }
            }

            if (!days.isEmpty()) {
                IPhoneData iPhoneData = (IPhoneData) phoneSelector.getSelectedItem();
                iPhoneData.exportToFile(days);
            }
        } else if (e.getActionCommand().equals(LOAD)) {
            populateTable((IPhoneData) phoneSelector.getSelectedItem());
        } else if (e.getActionCommand().equals(SWITCH_MAP_TILE)) {
            try {
                mapViewer.setTileSource((TileSource) tileSourceSelector.getSelectedItem());
            } catch (IllegalArgumentException ex) {
                logger.log(Level.WARNING, "Unable to set tile source", ex);
            }
        } else if (e.getActionCommand().equals(EXIT)) {
            System.exit(0);
        } else {
            throw new UnsupportedOperationException(e.getActionCommand()
                    + " isn't supported yet.");
        }
    }
}
