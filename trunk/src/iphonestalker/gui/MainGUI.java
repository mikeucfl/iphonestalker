/*
 *  This file is a part of iPhoneStalker.
 * 
 *  iPhoneStalker is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package iphonestalker.gui;

import iphonestalker.data.IPhoneRoute;
import iphonestalker.data.IPhoneLocation;
import iphonestalker.gui.interfaces.MapRoute;
import iphonestalker.util.BackupReader;
import iphonestalker.util.FindMyIPhoneReader;
import iphonestalker.util.io.InfoReader;
import iphonestalker.util.io.VersionCheck;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
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
    private static final String LOAD_BACKUP_DATA = "Load Backup Data";
    private static final String LOAD_FMI_DATA = "Load FMI Data";
    private static final String CONNECT_TO_FMI = "Connect to FMI";
    private static final String LIVE_VIEW_BOX = "Live View";
    private static final String SWITCH_MAP_TILE = "Switch map tile";
    private static final String EXPORT = "Export";
    private static final String EXIT = "Exit";
    private static final String FILE_SEPARATOR =
            System.getProperty("file.separator");
    private static final String FOLDER_PATH = "MobileSync" + FILE_SEPARATOR
            + "Backup" + FILE_SEPARATOR;
    private boolean initialized = false;
    private JMenuBar menuBar = null;
    private TrackerTableModel backupPhoneTableModel = null;
    private TrackerTableModel fmiPhoneTableModel = null;
    private JComboBox backupPhoneSelector = null;
    private JComboBox fmiPhoneSelector = null;
    private JTable backupPhoneTable = null;
    private JTable fmiPhoneTable = null;
    private BackupReader backupReader = null;
    private InfoReader infoReader = null;
    private MyJMapViewer mapViewer = null;
    private JComboBox tileSourceSelector = null;
    private JTextField statusBar = null;
    private String statusLog = "";
    private FindMyIPhoneReader findMyIPhoneReader = null;
    private JTextField username = null;
    private JPasswordField password = null;
    private Timer fmiTimer = null;
    private JCheckBox autoUpdateCb = null;

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
        findMyIPhoneReader = FindMyIPhoneReader.getInstance();
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

        if (VersionCheck.getLatestVersion() != null) {
            int reply = JOptionPane.showConfirmDialog(null,
                    "iPhoneStalker is out of date!\n"
                    + "Would you like to visit the iPhoneStalker homepage?",
                    "New Version!",
                    JOptionPane.YES_NO_OPTION);

            if (reply == JOptionPane.YES_OPTION) {
                if (!Desktop.isDesktopSupported()) {
                    JOptionPane.showMessageDialog(null, "Unable to open browser.\n"
                            + "Please visit http://iphonestalker.googlecode.com");
                } else {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        try {
                            desktop.browse(new URI("http://iphonestalker.googlecode.com"));

                        } catch (IOException ex) {
                            logger.log(Level.SEVERE, null, ex);
                            JOptionPane.showMessageDialog(null, "Unable to open browser.\n"
                                    + "Please visit http://iphonestalker.googlecode.com");
                        } catch (URISyntaxException ex) {
                            logger.log(Level.SEVERE, null, ex);
                            JOptionPane.showMessageDialog(null, "Unable to open browser.\n"
                                    + "Please visit http://iphonestalker.googlecode.com");
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Unable to open browser.\n"
                                + "Please visit http://iphonestalker.googlecode.com");
                    }
                }
            }
        }

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
        JPanel backupPane = new JPanel(new GridBagLayout());
        JPanel fmiPane = new JPanel(new GridBagLayout());
        JPanel fmiPaneLogin = new JPanel(new GridBagLayout());
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

        // Add the backup data pane
        JTabbedPane centerLeftTabbedPane = new JTabbedPane();
        centerLeftTabbedPane.addTab("Backup Data", null, backupPane);
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(10, 50, 0, 50);
        backupPhoneSelector = new JComboBox();
        backupPhoneSelector.setActionCommand(LOAD_BACKUP_DATA);
        backupPhoneSelector.addActionListener(this);
        backupPane.add(backupPhoneSelector, c);

        // Create the backup phone table model
        backupPhoneTableModel = new TrackerTableModel();
        backupPhoneTable = new SwapColorTable(backupPhoneTableModel);

        // Create the table sorter
        TableRowSorter<TrackerTableModel> sorter =
                new TableRowSorter<TrackerTableModel>(backupPhoneTableModel);

        // Comparator for sorting the first "index" column
        sorter.setComparator(0, new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        backupPhoneTable.setRowSorter(sorter);

        // Adjust the column of the table
        TableColumn col = backupPhoneTable.getColumnModel().getColumn(0);
        col.setPreferredWidth(1); // squeezes the other columns

        // Set attributes for the table
        backupPhoneTable.setSelectionBackground(new Color(255, 250, 205)); // light yellow
        backupPhoneTable.setSelectionForeground(Color.BLACK);
        backupPhoneTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        backupPhoneTable.getSelectionModel().addListSelectionListener(
                new LocationListSelectionListener(
                backupPhoneTable,
                backupPhoneSelector,
                backupPhoneTableModel));

        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 1.0;
        c.insets = new Insets(10, 5, 5, 5);
        backupPane.add(new JScrollPane(backupPhoneTable), c);

        // Add the find my iphone pane
        centerLeftTabbedPane.addTab("Find My iPhone", null, fmiPane);

        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0;
        c.weightx = 0;
        c.insets = new Insets(5, 5, 5, 5);
        fmiPaneLogin.add(new JLabel("Username:"), c);
        c.gridx = 1;
        username = new JTextField(10);
        username.addActionListener(this);
        username.setActionCommand(CONNECT_TO_FMI);
        c.weightx = 1;
        fmiPaneLogin.add(username, c);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        fmiPaneLogin.add(new JLabel("Password:"), c);
        c.gridx = 1;
        password = new JPasswordField();
        password.addActionListener(this);
        password.setActionCommand(CONNECT_TO_FMI);
        c.weightx = 0.6;
        fmiPaneLogin.add(password, c);
        JButton fmiAddButton = new JButton("Add");
        fmiAddButton.addActionListener(this);
        fmiAddButton.setActionCommand(CONNECT_TO_FMI);
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0.4;
        c.gridwidth = 1;
        c.gridheight = 2;
        fmiPaneLogin.add(fmiAddButton, c);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        fmiPane.add(fmiPaneLogin, c);

        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(5, 50, 0, 50);
        fmiPhoneSelector = new JComboBox();
        fmiPhoneSelector.setActionCommand(LOAD_FMI_DATA);
        fmiPhoneSelector.addActionListener(this);
        fmiPane.add(fmiPhoneSelector, c);
        c.gridy = 2;
        autoUpdateCb = new JCheckBox(LIVE_VIEW_BOX);
        autoUpdateCb.setSelected(true);
        autoUpdateCb.addActionListener(this);
        autoUpdateCb.setActionCommand(LIVE_VIEW_BOX);
        fmiPane.add(autoUpdateCb, c);

        // Create the backup phone table model
        fmiPhoneTableModel = new TrackerTableModel();
        fmiPhoneTable = new SwapColorTable(fmiPhoneTableModel);

        // Create the table sorter
        sorter = new TableRowSorter<TrackerTableModel>(fmiPhoneTableModel);

        // Comparator for sorting the first "index" column
        sorter.setComparator(0, new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        fmiPhoneTable.setRowSorter(sorter);

        // Adjust the column of the table
        col = fmiPhoneTable.getColumnModel().getColumn(0);
        col.setPreferredWidth(1); // squeezes the other columns

        // Set attributes for the table
        fmiPhoneTable.setSelectionBackground(new Color(255, 250, 205)); // light yellow
        fmiPhoneTable.setSelectionForeground(Color.BLACK);
        fmiPhoneTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fmiPhoneTable.getSelectionModel().addListSelectionListener(
                new LocationListSelectionListener(
                fmiPhoneTable,
                fmiPhoneSelector,
                fmiPhoneTableModel));

        c.gridx = 0;
        c.gridy = 3;
        c.weighty = 1.0;
        c.insets = new Insets(10, 5, 5, 5);
        fmiPane.add(new JScrollPane(fmiPhoneTable), c);

        // Add the map pane
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
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
                centerLeftTabbedPane, centerRightPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation((int) (getPreferredSize().width / 2.5));

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 1.0;
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
        bottomPane.add(statusBar, c);

        contentPane.add(bottomPane, BorderLayout.SOUTH);

        return contentPane;
    }

    private void populateTable(JTable table, TrackerTableModel tm, IPhoneRoute iPhoneData) {
        int[] selectedRows = table.getSelectedRows();
        tm.setData(iPhoneData);
        for (int row : selectedRows) {
            if (row < table.getRowCount()) {
                table.getSelectionModel().setSelectionInterval(row, row);
            }
        }
    }

    private void addBackupFolder(String backupFolder, boolean alertInfo) {
        File infoFile = new File(backupFolder + FILE_SEPARATOR + "Info.plist");
        if (infoFile.exists()) {
            IPhoneRoute iPhoneData =
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
                backupPhoneSelector.addItem(iPhoneData);
                File backupFolderFile = new File(backupFolder);
                addStatus("Loaded '" + iPhoneData + "' ["
                        + backupFolderFile.getName() + "]");
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
            int[] selectedRows = backupPhoneTable.getSelectedRows();

            ArrayList<String> days = new ArrayList<String>();
            if (selectedRows != null) {
                for (int row : selectedRows) {
                    days.add(backupPhoneTableModel.getDay(row));
                }
            }

            if (!days.isEmpty()) {
                IPhoneRoute iPhoneData = (IPhoneRoute) backupPhoneSelector.getSelectedItem();
                iPhoneData.exportToFile(days);
            }
        } else if (e.getActionCommand().equals(LOAD_BACKUP_DATA)) {
            final IPhoneRoute iPhoneRoute = (IPhoneRoute) backupPhoneSelector.getSelectedItem();
            populateTable(backupPhoneTable, backupPhoneTableModel, iPhoneRoute);
        } else if (e.getActionCommand().equals(SWITCH_MAP_TILE)) {
            try {
                mapViewer.setTileSource((TileSource) tileSourceSelector.getSelectedItem());
            } catch (IllegalArgumentException ex) {
                logger.log(Level.WARNING, "Unable to set tile source", ex);
            }
        } else if (e.getActionCommand().equals(EXIT)) {
            System.exit(0);
        } else if (e.getActionCommand().equals(CONNECT_TO_FMI)) {
            fmiPhoneSelector.removeAllItems();
            String error = findMyIPhoneReader.connect(username.getText(),
                    new String(password.getPassword()));
            if (error == null) {
                ArrayList<IPhoneRoute> iPhoneRouteList =
                        (ArrayList<IPhoneRoute>) findMyIPhoneReader.getIPhoneRouteList();

                for (IPhoneRoute iPhoneRoute : iPhoneRouteList) {
                    fmiPhoneSelector.addItem(iPhoneRoute);
                    addStatus("Loaded '" + iPhoneRoute.toString());
                }
            } else {
                addStatus(error);
            }
        } else if (e.getActionCommand().equals(LOAD_FMI_DATA)) {
            final IPhoneRoute iPhoneRoute = (IPhoneRoute) fmiPhoneSelector.getSelectedItem();
            if (iPhoneRoute != null) {
                populateTable(fmiPhoneTable, fmiPhoneTableModel, iPhoneRoute);
            }

            toggleLiveFmiUpdate();
        } else if (e.getActionCommand().equals(LIVE_VIEW_BOX)) {
            toggleLiveFmiUpdate();
        } else {
            throw new UnsupportedOperationException(e.getActionCommand()
                    + " isn't supported yet.");
        }
    }

    private class SwapColorTable extends JTable {

        public SwapColorTable(TableModel tm) {
            super(tm);
        }

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
    }

    private void toggleLiveFmiUpdate() {
        stopFmiUpdate();
        if (autoUpdateCb.isSelected()) {
            startFmiUpdate();
        }
    }

    private void stopFmiUpdate() {
        if (fmiTimer != null) {
            fmiTimer.cancel();
            fmiTimer.purge();
        }
    }

    private void startFmiUpdate() {
        if (fmiPhoneSelector.getSelectedItem() != null) {
            final IPhoneRoute iPhoneRoute = (IPhoneRoute) fmiPhoneSelector.getSelectedItem();
            String name = fmiPhoneSelector.getSelectedItem().toString();
            final int device = Integer.parseInt(name.split("#")[1].split("]")[0]) - 1;
            fmiTimer = new Timer(name + " PollingThread");
            fmiTimer.scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                    findMyIPhoneReader.pollLocation(device);
                    populateTable(fmiPhoneTable, fmiPhoneTableModel, iPhoneRoute);
                }
            }, 0, 5000);
        }
    }

    private class LocationListSelectionListener implements ListSelectionListener {

        private JTable table = null;
        private JComboBox selector = null;
        private TrackerTableModel tm = null;

        public LocationListSelectionListener(JTable table, JComboBox selector, TrackerTableModel tm) {
            this.table = table;
            this.selector = selector;
            this.tm = tm;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                int[] selectedRows = table.getSelectedRows();
                int[] sortedRows = null;

                if (selectedRows != null) {
                    sortedRows = new int[selectedRows.length];
                    for (int i = 0; i < selectedRows.length; i++) {
                        sortedRows[i] = table.convertRowIndexToModel(selectedRows[i]);
                    }
                }

                IPhoneRoute iPhoneData = (IPhoneRoute) selector.getSelectedItem();

                ArrayList<MapRoute> mapRouteList = new ArrayList<MapRoute>();
                if (sortedRows != null) {
                    for (int row : sortedRows) {

                        String day = tm.getDay(row);

                        // Get the map route
                        ArrayList<IPhoneLocation> iPhoneLocations =
                                iPhoneData.getIPhoneLocations(day);

                        MyMapRoute mapRoute =
                                new MyMapRoute(
                                null,
                                Color.BLUE,
                                iPhoneLocations);
                        mapRouteList.add(mapRoute);
                    }
                }

                mapViewer.setMapRouteList(mapRouteList);

                if (!mapRouteList.isEmpty()) {
                    if (iPhoneData.isFMI()) {
                        MyMapRoute mapRoute = (MyMapRoute) mapRouteList.get(mapRouteList.size() - 1);
                        List<MapRoute> displayMapRouteList = new ArrayList<MapRoute>();
                        displayMapRouteList.add(mapRoute);
                        mapViewer.setDisplayToFitMapRoutes(displayMapRouteList);
                    } else {
                        mapViewer.setDisplayToFitMapRoutes(null);
                    }
                }
            }
        }
    }
}
