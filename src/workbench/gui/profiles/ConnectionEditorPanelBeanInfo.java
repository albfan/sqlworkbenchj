package workbench.gui.profiles;

import java.beans.*;

public class ConnectionEditorPanelBeanInfo extends SimpleBeanInfo
{

    // Bean descriptor //GEN-FIRST:BeanDescriptor
    /*lazy BeanDescriptor*/
    private static BeanDescriptor getBdescriptor(){
        BeanDescriptor beanDescriptor = new BeanDescriptor  ( ConnectionEditorPanel.class , null );
        beanDescriptor.setDisplayName ( "Connection editor" );//GEN-HEADEREND:BeanDescriptor

		// Here you can add code for customizing the BeanDescriptor.

        return beanDescriptor;         }//GEN-LAST:BeanDescriptor


    // Property identifiers //GEN-FIRST:Properties
    private static final int PROPERTY_registeredKeyStrokes = 0;
    private static final int PROPERTY_valid = 1;
    private static final int PROPERTY_y = 2;
    private static final int PROPERTY_insets = 3;
    private static final int PROPERTY_focusCycleRoot = 4;
    private static final int PROPERTY_maximumSizeSet = 5;
    private static final int PROPERTY_preferredSizeSet = 6;
    private static final int PROPERTY_UIClassID = 7;
    private static final int PROPERTY_verifyInputWhenFocusTarget = 8;
    private static final int PROPERTY_propertyChangeListeners = 9;
    private static final int PROPERTY_alignmentY = 10;
    private static final int PROPERTY_doubleBuffered = 11;
    private static final int PROPERTY_font = 12;
    private static final int PROPERTY_drivers = 13;
    private static final int PROPERTY_focusListeners = 14;
    private static final int PROPERTY_width = 15;
    private static final int PROPERTY_mouseMotionListeners = 16;
    private static final int PROPERTY_foreground = 17;
    private static final int PROPERTY_componentListeners = 18;
    private static final int PROPERTY_enabled = 19;
    private static final int PROPERTY_maximumSize = 20;
    private static final int PROPERTY_debugGraphicsOptions = 21;
    private static final int PROPERTY_inputVerifier = 22;
    private static final int PROPERTY_containerListeners = 23;
    private static final int PROPERTY_focusTraversable = 24;
    private static final int PROPERTY_toolTipText = 25;
    private static final int PROPERTY_inputMethodRequests = 26;
    private static final int PROPERTY_minimumSize = 27;
    private static final int PROPERTY_ancestorListeners = 28;
    private static final int PROPERTY_graphicsConfiguration = 29;
    private static final int PROPERTY_parent = 30;
    private static final int PROPERTY_focusTraversalPolicySet = 31;
    private static final int PROPERTY_mouseWheelListeners = 32;
    private static final int PROPERTY_height = 33;
    private static final int PROPERTY_opaque = 34;
    private static final int PROPERTY_keyListeners = 35;
    private static final int PROPERTY_foregroundSet = 36;
    private static final int PROPERTY_accessibleContext = 37;
    private static final int PROPERTY_focusTraversalPolicy = 38;
    private static final int PROPERTY_hierarchyBoundsListeners = 39;
    private static final int PROPERTY_UI = 40;
    private static final int PROPERTY_paintingTile = 41;
    private static final int PROPERTY_vetoableChangeListeners = 42;
    private static final int PROPERTY_hierarchyListeners = 43;
    private static final int PROPERTY_focusTraversalKeysEnabled = 44;
    private static final int PROPERTY_colorModel = 45;
    private static final int PROPERTY_x = 46;
    private static final int PROPERTY_requestFocusEnabled = 47;
    private static final int PROPERTY_visibleRect = 48;
    private static final int PROPERTY_visible = 49;
    private static final int PROPERTY_rootPane = 50;
    private static final int PROPERTY_treeLock = 51;
    private static final int PROPERTY_focusCycleRootAncestor = 52;
    private static final int PROPERTY_peer = 53;
    private static final int PROPERTY_dropTarget = 54;
    private static final int PROPERTY_transferHandler = 55;
    private static final int PROPERTY_locale = 56;
    private static final int PROPERTY_ignoreRepaint = 57;
    private static final int PROPERTY_cursor = 58;
    private static final int PROPERTY_alignmentX = 59;
    private static final int PROPERTY_backgroundSet = 60;
    private static final int PROPERTY_optimizedDrawingEnabled = 61;
    private static final int PROPERTY_actionMap = 62;
    private static final int PROPERTY_showing = 63;
    private static final int PROPERTY_toolkit = 64;
    private static final int PROPERTY_nextFocusableComponent = 65;
    private static final int PROPERTY_focusOwner = 66;
    private static final int PROPERTY_autoscrolls = 67;
    private static final int PROPERTY_bounds = 68;
    private static final int PROPERTY_inputMethodListeners = 69;
    private static final int PROPERTY_minimumSizeSet = 70;
    private static final int PROPERTY_focusable = 71;
    private static final int PROPERTY_background = 72;
    private static final int PROPERTY_cursorSet = 73;
    private static final int PROPERTY_border = 74;
    private static final int PROPERTY_layout = 75;
    private static final int PROPERTY_profile = 76;
    private static final int PROPERTY_topLevelAncestor = 77;
    private static final int PROPERTY_preferredSize = 78;
    private static final int PROPERTY_displayable = 79;
    private static final int PROPERTY_mouseListeners = 80;
    private static final int PROPERTY_validateRoot = 81;
    private static final int PROPERTY_components = 82;
    private static final int PROPERTY_managingFocus = 83;
    private static final int PROPERTY_componentOrientation = 84;
    private static final int PROPERTY_fontSet = 85;
    private static final int PROPERTY_componentCount = 86;
    private static final int PROPERTY_lightweight = 87;
    private static final int PROPERTY_name = 88;
    private static final int PROPERTY_graphics = 89;
    private static final int PROPERTY_inputContext = 90;
    private static final int PROPERTY_locationOnScreen = 91;
    private static final int PROPERTY_component = 92;
    private static final int PROPERTY_focusTraversalKeys = 93;

    // Property array
    /*lazy PropertyDescriptor*/
    private static PropertyDescriptor[] getPdescriptor(){
        PropertyDescriptor[] properties = new PropertyDescriptor[94];

        try {
            properties[PROPERTY_registeredKeyStrokes] = new PropertyDescriptor ( "registeredKeyStrokes", ConnectionEditorPanel.class, "getRegisteredKeyStrokes", null );
            properties[PROPERTY_valid] = new PropertyDescriptor ( "valid", ConnectionEditorPanel.class, "isValid", null );
            properties[PROPERTY_y] = new PropertyDescriptor ( "y", ConnectionEditorPanel.class, "getY", null );
            properties[PROPERTY_insets] = new PropertyDescriptor ( "insets", ConnectionEditorPanel.class, "getInsets", null );
            properties[PROPERTY_focusCycleRoot] = new PropertyDescriptor ( "focusCycleRoot", ConnectionEditorPanel.class, "isFocusCycleRoot", "setFocusCycleRoot" );
            properties[PROPERTY_maximumSizeSet] = new PropertyDescriptor ( "maximumSizeSet", ConnectionEditorPanel.class, "isMaximumSizeSet", null );
            properties[PROPERTY_preferredSizeSet] = new PropertyDescriptor ( "preferredSizeSet", ConnectionEditorPanel.class, "isPreferredSizeSet", null );
            properties[PROPERTY_UIClassID] = new PropertyDescriptor ( "UIClassID", ConnectionEditorPanel.class, "getUIClassID", null );
            properties[PROPERTY_verifyInputWhenFocusTarget] = new PropertyDescriptor ( "verifyInputWhenFocusTarget", ConnectionEditorPanel.class, "getVerifyInputWhenFocusTarget", "setVerifyInputWhenFocusTarget" );
            properties[PROPERTY_propertyChangeListeners] = new PropertyDescriptor ( "propertyChangeListeners", ConnectionEditorPanel.class, "getPropertyChangeListeners", null );
            properties[PROPERTY_alignmentY] = new PropertyDescriptor ( "alignmentY", ConnectionEditorPanel.class, "getAlignmentY", "setAlignmentY" );
            properties[PROPERTY_doubleBuffered] = new PropertyDescriptor ( "doubleBuffered", ConnectionEditorPanel.class, "isDoubleBuffered", "setDoubleBuffered" );
            properties[PROPERTY_font] = new PropertyDescriptor ( "font", ConnectionEditorPanel.class, "getFont", "setFont" );
            properties[PROPERTY_drivers] = new PropertyDescriptor ( "drivers", ConnectionEditorPanel.class, null, "setDrivers" );
            properties[PROPERTY_focusListeners] = new PropertyDescriptor ( "focusListeners", ConnectionEditorPanel.class, "getFocusListeners", null );
            properties[PROPERTY_width] = new PropertyDescriptor ( "width", ConnectionEditorPanel.class, "getWidth", null );
            properties[PROPERTY_mouseMotionListeners] = new PropertyDescriptor ( "mouseMotionListeners", ConnectionEditorPanel.class, "getMouseMotionListeners", null );
            properties[PROPERTY_foreground] = new PropertyDescriptor ( "foreground", ConnectionEditorPanel.class, "getForeground", "setForeground" );
            properties[PROPERTY_componentListeners] = new PropertyDescriptor ( "componentListeners", ConnectionEditorPanel.class, "getComponentListeners", null );
            properties[PROPERTY_enabled] = new PropertyDescriptor ( "enabled", ConnectionEditorPanel.class, "isEnabled", "setEnabled" );
            properties[PROPERTY_maximumSize] = new PropertyDescriptor ( "maximumSize", ConnectionEditorPanel.class, "getMaximumSize", "setMaximumSize" );
            properties[PROPERTY_debugGraphicsOptions] = new PropertyDescriptor ( "debugGraphicsOptions", ConnectionEditorPanel.class, "getDebugGraphicsOptions", "setDebugGraphicsOptions" );
            properties[PROPERTY_inputVerifier] = new PropertyDescriptor ( "inputVerifier", ConnectionEditorPanel.class, "getInputVerifier", "setInputVerifier" );
            properties[PROPERTY_containerListeners] = new PropertyDescriptor ( "containerListeners", ConnectionEditorPanel.class, "getContainerListeners", null );
            properties[PROPERTY_focusTraversable] = new PropertyDescriptor ( "focusTraversable", ConnectionEditorPanel.class, "isFocusTraversable", null );
            properties[PROPERTY_toolTipText] = new PropertyDescriptor ( "toolTipText", ConnectionEditorPanel.class, "getToolTipText", "setToolTipText" );
            properties[PROPERTY_inputMethodRequests] = new PropertyDescriptor ( "inputMethodRequests", ConnectionEditorPanel.class, "getInputMethodRequests", null );
            properties[PROPERTY_minimumSize] = new PropertyDescriptor ( "minimumSize", ConnectionEditorPanel.class, "getMinimumSize", "setMinimumSize" );
            properties[PROPERTY_ancestorListeners] = new PropertyDescriptor ( "ancestorListeners", ConnectionEditorPanel.class, "getAncestorListeners", null );
            properties[PROPERTY_graphicsConfiguration] = new PropertyDescriptor ( "graphicsConfiguration", ConnectionEditorPanel.class, "getGraphicsConfiguration", null );
            properties[PROPERTY_parent] = new PropertyDescriptor ( "parent", ConnectionEditorPanel.class, "getParent", null );
            properties[PROPERTY_focusTraversalPolicySet] = new PropertyDescriptor ( "focusTraversalPolicySet", ConnectionEditorPanel.class, "isFocusTraversalPolicySet", null );
            properties[PROPERTY_mouseWheelListeners] = new PropertyDescriptor ( "mouseWheelListeners", ConnectionEditorPanel.class, "getMouseWheelListeners", null );
            properties[PROPERTY_height] = new PropertyDescriptor ( "height", ConnectionEditorPanel.class, "getHeight", null );
            properties[PROPERTY_opaque] = new PropertyDescriptor ( "opaque", ConnectionEditorPanel.class, "isOpaque", "setOpaque" );
            properties[PROPERTY_keyListeners] = new PropertyDescriptor ( "keyListeners", ConnectionEditorPanel.class, "getKeyListeners", null );
            properties[PROPERTY_foregroundSet] = new PropertyDescriptor ( "foregroundSet", ConnectionEditorPanel.class, "isForegroundSet", null );
            properties[PROPERTY_accessibleContext] = new PropertyDescriptor ( "accessibleContext", ConnectionEditorPanel.class, "getAccessibleContext", null );
            properties[PROPERTY_focusTraversalPolicy] = new PropertyDescriptor ( "focusTraversalPolicy", ConnectionEditorPanel.class, "getFocusTraversalPolicy", "setFocusTraversalPolicy" );
            properties[PROPERTY_hierarchyBoundsListeners] = new PropertyDescriptor ( "hierarchyBoundsListeners", ConnectionEditorPanel.class, "getHierarchyBoundsListeners", null );
            properties[PROPERTY_UI] = new PropertyDescriptor ( "UI", ConnectionEditorPanel.class, "getUI", "setUI" );
            properties[PROPERTY_paintingTile] = new PropertyDescriptor ( "paintingTile", ConnectionEditorPanel.class, "isPaintingTile", null );
            properties[PROPERTY_vetoableChangeListeners] = new PropertyDescriptor ( "vetoableChangeListeners", ConnectionEditorPanel.class, "getVetoableChangeListeners", null );
            properties[PROPERTY_hierarchyListeners] = new PropertyDescriptor ( "hierarchyListeners", ConnectionEditorPanel.class, "getHierarchyListeners", null );
            properties[PROPERTY_focusTraversalKeysEnabled] = new PropertyDescriptor ( "focusTraversalKeysEnabled", ConnectionEditorPanel.class, "getFocusTraversalKeysEnabled", "setFocusTraversalKeysEnabled" );
            properties[PROPERTY_colorModel] = new PropertyDescriptor ( "colorModel", ConnectionEditorPanel.class, "getColorModel", null );
            properties[PROPERTY_x] = new PropertyDescriptor ( "x", ConnectionEditorPanel.class, "getX", null );
            properties[PROPERTY_requestFocusEnabled] = new PropertyDescriptor ( "requestFocusEnabled", ConnectionEditorPanel.class, "isRequestFocusEnabled", "setRequestFocusEnabled" );
            properties[PROPERTY_visibleRect] = new PropertyDescriptor ( "visibleRect", ConnectionEditorPanel.class, "getVisibleRect", null );
            properties[PROPERTY_visible] = new PropertyDescriptor ( "visible", ConnectionEditorPanel.class, "isVisible", "setVisible" );
            properties[PROPERTY_rootPane] = new PropertyDescriptor ( "rootPane", ConnectionEditorPanel.class, "getRootPane", null );
            properties[PROPERTY_treeLock] = new PropertyDescriptor ( "treeLock", ConnectionEditorPanel.class, "getTreeLock", null );
            properties[PROPERTY_focusCycleRootAncestor] = new PropertyDescriptor ( "focusCycleRootAncestor", ConnectionEditorPanel.class, "getFocusCycleRootAncestor", null );
            properties[PROPERTY_peer] = new PropertyDescriptor ( "peer", ConnectionEditorPanel.class, "getPeer", null );
            properties[PROPERTY_dropTarget] = new PropertyDescriptor ( "dropTarget", ConnectionEditorPanel.class, "getDropTarget", "setDropTarget" );
            properties[PROPERTY_transferHandler] = new PropertyDescriptor ( "transferHandler", ConnectionEditorPanel.class, "getTransferHandler", "setTransferHandler" );
            properties[PROPERTY_locale] = new PropertyDescriptor ( "locale", ConnectionEditorPanel.class, "getLocale", "setLocale" );
            properties[PROPERTY_ignoreRepaint] = new PropertyDescriptor ( "ignoreRepaint", ConnectionEditorPanel.class, "getIgnoreRepaint", "setIgnoreRepaint" );
            properties[PROPERTY_cursor] = new PropertyDescriptor ( "cursor", ConnectionEditorPanel.class, "getCursor", "setCursor" );
            properties[PROPERTY_alignmentX] = new PropertyDescriptor ( "alignmentX", ConnectionEditorPanel.class, "getAlignmentX", "setAlignmentX" );
            properties[PROPERTY_backgroundSet] = new PropertyDescriptor ( "backgroundSet", ConnectionEditorPanel.class, "isBackgroundSet", null );
            properties[PROPERTY_optimizedDrawingEnabled] = new PropertyDescriptor ( "optimizedDrawingEnabled", ConnectionEditorPanel.class, "isOptimizedDrawingEnabled", null );
            properties[PROPERTY_actionMap] = new PropertyDescriptor ( "actionMap", ConnectionEditorPanel.class, "getActionMap", "setActionMap" );
            properties[PROPERTY_showing] = new PropertyDescriptor ( "showing", ConnectionEditorPanel.class, "isShowing", null );
            properties[PROPERTY_toolkit] = new PropertyDescriptor ( "toolkit", ConnectionEditorPanel.class, "getToolkit", null );
            properties[PROPERTY_nextFocusableComponent] = new PropertyDescriptor ( "nextFocusableComponent", ConnectionEditorPanel.class, "getNextFocusableComponent", "setNextFocusableComponent" );
            properties[PROPERTY_focusOwner] = new PropertyDescriptor ( "focusOwner", ConnectionEditorPanel.class, "isFocusOwner", null );
            properties[PROPERTY_autoscrolls] = new PropertyDescriptor ( "autoscrolls", ConnectionEditorPanel.class, "getAutoscrolls", "setAutoscrolls" );
            properties[PROPERTY_bounds] = new PropertyDescriptor ( "bounds", ConnectionEditorPanel.class, "getBounds", "setBounds" );
            properties[PROPERTY_inputMethodListeners] = new PropertyDescriptor ( "inputMethodListeners", ConnectionEditorPanel.class, "getInputMethodListeners", null );
            properties[PROPERTY_minimumSizeSet] = new PropertyDescriptor ( "minimumSizeSet", ConnectionEditorPanel.class, "isMinimumSizeSet", null );
            properties[PROPERTY_focusable] = new PropertyDescriptor ( "focusable", ConnectionEditorPanel.class, "isFocusable", "setFocusable" );
            properties[PROPERTY_background] = new PropertyDescriptor ( "background", ConnectionEditorPanel.class, "getBackground", "setBackground" );
            properties[PROPERTY_cursorSet] = new PropertyDescriptor ( "cursorSet", ConnectionEditorPanel.class, "isCursorSet", null );
            properties[PROPERTY_border] = new PropertyDescriptor ( "border", ConnectionEditorPanel.class, "getBorder", "setBorder" );
            properties[PROPERTY_layout] = new PropertyDescriptor ( "layout", ConnectionEditorPanel.class, "getLayout", "setLayout" );
            properties[PROPERTY_profile] = new PropertyDescriptor ( "profile", ConnectionEditorPanel.class, "getProfile", "setProfile" );
            properties[PROPERTY_topLevelAncestor] = new PropertyDescriptor ( "topLevelAncestor", ConnectionEditorPanel.class, "getTopLevelAncestor", null );
            properties[PROPERTY_preferredSize] = new PropertyDescriptor ( "preferredSize", ConnectionEditorPanel.class, "getPreferredSize", "setPreferredSize" );
            properties[PROPERTY_displayable] = new PropertyDescriptor ( "displayable", ConnectionEditorPanel.class, "isDisplayable", null );
            properties[PROPERTY_mouseListeners] = new PropertyDescriptor ( "mouseListeners", ConnectionEditorPanel.class, "getMouseListeners", null );
            properties[PROPERTY_validateRoot] = new PropertyDescriptor ( "validateRoot", ConnectionEditorPanel.class, "isValidateRoot", null );
            properties[PROPERTY_components] = new PropertyDescriptor ( "components", ConnectionEditorPanel.class, "getComponents", null );
            properties[PROPERTY_managingFocus] = new PropertyDescriptor ( "managingFocus", ConnectionEditorPanel.class, "isManagingFocus", null );
            properties[PROPERTY_componentOrientation] = new PropertyDescriptor ( "componentOrientation", ConnectionEditorPanel.class, "getComponentOrientation", "setComponentOrientation" );
            properties[PROPERTY_fontSet] = new PropertyDescriptor ( "fontSet", ConnectionEditorPanel.class, "isFontSet", null );
            properties[PROPERTY_componentCount] = new PropertyDescriptor ( "componentCount", ConnectionEditorPanel.class, "getComponentCount", null );
            properties[PROPERTY_lightweight] = new PropertyDescriptor ( "lightweight", ConnectionEditorPanel.class, "isLightweight", null );
            properties[PROPERTY_name] = new PropertyDescriptor ( "name", ConnectionEditorPanel.class, "getName", "setName" );
            properties[PROPERTY_graphics] = new PropertyDescriptor ( "graphics", ConnectionEditorPanel.class, "getGraphics", null );
            properties[PROPERTY_inputContext] = new PropertyDescriptor ( "inputContext", ConnectionEditorPanel.class, "getInputContext", null );
            properties[PROPERTY_locationOnScreen] = new PropertyDescriptor ( "locationOnScreen", ConnectionEditorPanel.class, "getLocationOnScreen", null );
            properties[PROPERTY_component] = new IndexedPropertyDescriptor ( "component", ConnectionEditorPanel.class, null, null, "getComponent", null );
            properties[PROPERTY_focusTraversalKeys] = new IndexedPropertyDescriptor ( "focusTraversalKeys", ConnectionEditorPanel.class, null, null, "getFocusTraversalKeys", "setFocusTraversalKeys" );
        }
        catch( IntrospectionException e) {}//GEN-HEADEREND:Properties

		// Here you can add code for customizing the properties array.

        return properties;         }//GEN-LAST:Properties

    // EventSet identifiers//GEN-FIRST:Events
    private static final int EVENT_ancestorListener = 0;
    private static final int EVENT_mouseWheelListener = 1;
    private static final int EVENT_propertyChangeListener = 2;
    private static final int EVENT_componentListener = 3;
    private static final int EVENT_hierarchyListener = 4;
    private static final int EVENT_mouseMotionListener = 5;
    private static final int EVENT_focusListener = 6;
    private static final int EVENT_containerListener = 7;
    private static final int EVENT_keyListener = 8;
    private static final int EVENT_mouseListener = 9;
    private static final int EVENT_vetoableChangeListener = 10;
    private static final int EVENT_hierarchyBoundsListener = 11;
    private static final int EVENT_inputMethodListener = 12;

    // EventSet array
    /*lazy EventSetDescriptor*/
    private static EventSetDescriptor[] getEdescriptor(){
        EventSetDescriptor[] eventSets = new EventSetDescriptor[13];

            try {
            eventSets[EVENT_ancestorListener] = new EventSetDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class, "ancestorListener", javax.swing.event.AncestorListener.class, new String[] {"ancestorAdded", "ancestorRemoved", "ancestorMoved"}, "addAncestorListener", "removeAncestorListener" );
            eventSets[EVENT_mouseWheelListener] = new EventSetDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class, "mouseWheelListener", java.awt.event.MouseWheelListener.class, new String[] {"mouseWheelMoved"}, "addMouseWheelListener", "removeMouseWheelListener" );
            eventSets[EVENT_propertyChangeListener] = new EventSetDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class, "propertyChangeListener", java.beans.PropertyChangeListener.class, new String[] {"propertyChange"}, "addPropertyChangeListener", "removePropertyChangeListener" );
            eventSets[EVENT_componentListener] = new EventSetDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class, "componentListener", java.awt.event.ComponentListener.class, new String[] {"componentResized", "componentMoved", "componentShown", "componentHidden"}, "addComponentListener", "removeComponentListener" );
            eventSets[EVENT_hierarchyListener] = new EventSetDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class, "hierarchyListener", java.awt.event.HierarchyListener.class, new String[] {"hierarchyChanged"}, "addHierarchyListener", "removeHierarchyListener" );
            eventSets[EVENT_mouseMotionListener] = new EventSetDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class, "mouseMotionListener", java.awt.event.MouseMotionListener.class, new String[] {"mouseDragged", "mouseMoved"}, "addMouseMotionListener", "removeMouseMotionListener" );
            eventSets[EVENT_focusListener] = new EventSetDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class, "focusListener", java.awt.event.FocusListener.class, new String[] {"focusGained", "focusLost"}, "addFocusListener", "removeFocusListener" );
            eventSets[EVENT_containerListener] = new EventSetDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class, "containerListener", java.awt.event.ContainerListener.class, new String[] {"componentAdded", "componentRemoved"}, "addContainerListener", "removeContainerListener" );
            eventSets[EVENT_keyListener] = new EventSetDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class, "keyListener", java.awt.event.KeyListener.class, new String[] {"keyTyped", "keyPressed", "keyReleased"}, "addKeyListener", "removeKeyListener" );
            eventSets[EVENT_mouseListener] = new EventSetDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class, "mouseListener", java.awt.event.MouseListener.class, new String[] {"mouseClicked", "mousePressed", "mouseReleased", "mouseEntered", "mouseExited"}, "addMouseListener", "removeMouseListener" );
            eventSets[EVENT_vetoableChangeListener] = new EventSetDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class, "vetoableChangeListener", java.beans.VetoableChangeListener.class, new String[] {"vetoableChange"}, "addVetoableChangeListener", "removeVetoableChangeListener" );
            eventSets[EVENT_hierarchyBoundsListener] = new EventSetDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class, "hierarchyBoundsListener", java.awt.event.HierarchyBoundsListener.class, new String[] {"ancestorMoved", "ancestorResized"}, "addHierarchyBoundsListener", "removeHierarchyBoundsListener" );
            eventSets[EVENT_inputMethodListener] = new EventSetDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class, "inputMethodListener", java.awt.event.InputMethodListener.class, new String[] {"inputMethodTextChanged", "caretPositionChanged"}, "addInputMethodListener", "removeInputMethodListener" );
        }
        catch( IntrospectionException e) {}//GEN-HEADEREND:Events

		// Here you can add code for customizing the event sets array.

        return eventSets;         }//GEN-LAST:Events

    // Method identifiers //GEN-FIRST:Methods
    private static final int METHOD_updateUI0 = 0;
    private static final int METHOD_update1 = 1;
    private static final int METHOD_paint2 = 2;
    private static final int METHOD_printAll3 = 3;
    private static final int METHOD_print4 = 4;
    private static final int METHOD_requestFocus5 = 5;
    private static final int METHOD_requestFocus6 = 6;
    private static final int METHOD_requestFocusInWindow7 = 7;
    private static final int METHOD_grabFocus8 = 8;
    private static final int METHOD_contains9 = 9;
    private static final int METHOD_getInsets10 = 10;
    private static final int METHOD_registerKeyboardAction11 = 11;
    private static final int METHOD_registerKeyboardAction12 = 12;
    private static final int METHOD_unregisterKeyboardAction13 = 13;
    private static final int METHOD_getConditionForKeyStroke14 = 14;
    private static final int METHOD_getActionForKeyStroke15 = 15;
    private static final int METHOD_resetKeyboardActions16 = 16;
    private static final int METHOD_setInputMap17 = 17;
    private static final int METHOD_getInputMap18 = 18;
    private static final int METHOD_getInputMap19 = 19;
    private static final int METHOD_requestDefaultFocus20 = 20;
    private static final int METHOD_getDefaultLocale21 = 21;
    private static final int METHOD_setDefaultLocale22 = 22;
    private static final int METHOD_getToolTipText23 = 23;
    private static final int METHOD_getToolTipLocation24 = 24;
    private static final int METHOD_createToolTip25 = 25;
    private static final int METHOD_scrollRectToVisible26 = 26;
    private static final int METHOD_enable27 = 27;
    private static final int METHOD_disable28 = 28;
    private static final int METHOD_getClientProperty29 = 29;
    private static final int METHOD_putClientProperty30 = 30;
    private static final int METHOD_isLightweightComponent31 = 31;
    private static final int METHOD_reshape32 = 32;
    private static final int METHOD_getBounds33 = 33;
    private static final int METHOD_getSize34 = 34;
    private static final int METHOD_getLocation35 = 35;
    private static final int METHOD_computeVisibleRect36 = 36;
    private static final int METHOD_firePropertyChange37 = 37;
    private static final int METHOD_firePropertyChange38 = 38;
    private static final int METHOD_firePropertyChange39 = 39;
    private static final int METHOD_firePropertyChange40 = 40;
    private static final int METHOD_firePropertyChange41 = 41;
    private static final int METHOD_firePropertyChange42 = 42;
    private static final int METHOD_firePropertyChange43 = 43;
    private static final int METHOD_firePropertyChange44 = 44;
    private static final int METHOD_addPropertyChangeListener45 = 45;
    private static final int METHOD_removePropertyChangeListener46 = 46;
    private static final int METHOD_getPropertyChangeListeners47 = 47;
    private static final int METHOD_getListeners48 = 48;
    private static final int METHOD_addNotify49 = 49;
    private static final int METHOD_removeNotify50 = 50;
    private static final int METHOD_repaint51 = 51;
    private static final int METHOD_repaint52 = 52;
    private static final int METHOD_revalidate53 = 53;
    private static final int METHOD_paintImmediately54 = 54;
    private static final int METHOD_paintImmediately55 = 55;
    private static final int METHOD_countComponents56 = 56;
    private static final int METHOD_insets57 = 57;
    private static final int METHOD_add58 = 58;
    private static final int METHOD_add59 = 59;
    private static final int METHOD_add60 = 60;
    private static final int METHOD_add61 = 61;
    private static final int METHOD_add62 = 62;
    private static final int METHOD_remove63 = 63;
    private static final int METHOD_remove64 = 64;
    private static final int METHOD_removeAll65 = 65;
    private static final int METHOD_doLayout66 = 66;
    private static final int METHOD_layout67 = 67;
    private static final int METHOD_invalidate68 = 68;
    private static final int METHOD_validate69 = 69;
    private static final int METHOD_preferredSize70 = 70;
    private static final int METHOD_minimumSize71 = 71;
    private static final int METHOD_paintComponents72 = 72;
    private static final int METHOD_printComponents73 = 73;
    private static final int METHOD_deliverEvent74 = 74;
    private static final int METHOD_getComponentAt75 = 75;
    private static final int METHOD_locate76 = 76;
    private static final int METHOD_getComponentAt77 = 77;
    private static final int METHOD_findComponentAt78 = 78;
    private static final int METHOD_findComponentAt79 = 79;
    private static final int METHOD_isAncestorOf80 = 80;
    private static final int METHOD_list81 = 81;
    private static final int METHOD_list82 = 82;
    private static final int METHOD_areFocusTraversalKeysSet83 = 83;
    private static final int METHOD_isFocusCycleRoot84 = 84;
    private static final int METHOD_transferFocusBackward85 = 85;
    private static final int METHOD_transferFocusDownCycle86 = 86;
    private static final int METHOD_applyComponentOrientation87 = 87;
    private static final int METHOD_enable88 = 88;
    private static final int METHOD_enableInputMethods89 = 89;
    private static final int METHOD_show90 = 90;
    private static final int METHOD_show91 = 91;
    private static final int METHOD_hide92 = 92;
    private static final int METHOD_getLocation93 = 93;
    private static final int METHOD_location94 = 94;
    private static final int METHOD_setLocation95 = 95;
    private static final int METHOD_move96 = 96;
    private static final int METHOD_setLocation97 = 97;
    private static final int METHOD_getSize98 = 98;
    private static final int METHOD_size99 = 99;
    private static final int METHOD_setSize100 = 100;
    private static final int METHOD_resize101 = 101;
    private static final int METHOD_setSize102 = 102;
    private static final int METHOD_resize103 = 103;
    private static final int METHOD_bounds104 = 104;
    private static final int METHOD_setBounds105 = 105;
    private static final int METHOD_getFontMetrics106 = 106;
    private static final int METHOD_paintAll107 = 107;
    private static final int METHOD_repaint108 = 108;
    private static final int METHOD_repaint109 = 109;
    private static final int METHOD_repaint110 = 110;
    private static final int METHOD_imageUpdate111 = 111;
    private static final int METHOD_createImage112 = 112;
    private static final int METHOD_createImage113 = 113;
    private static final int METHOD_createVolatileImage114 = 114;
    private static final int METHOD_createVolatileImage115 = 115;
    private static final int METHOD_prepareImage116 = 116;
    private static final int METHOD_prepareImage117 = 117;
    private static final int METHOD_checkImage118 = 118;
    private static final int METHOD_checkImage119 = 119;
    private static final int METHOD_inside120 = 120;
    private static final int METHOD_contains121 = 121;
    private static final int METHOD_dispatchEvent122 = 122;
    private static final int METHOD_postEvent123 = 123;
    private static final int METHOD_handleEvent124 = 124;
    private static final int METHOD_mouseDown125 = 125;
    private static final int METHOD_mouseDrag126 = 126;
    private static final int METHOD_mouseUp127 = 127;
    private static final int METHOD_mouseMove128 = 128;
    private static final int METHOD_mouseEnter129 = 129;
    private static final int METHOD_mouseExit130 = 130;
    private static final int METHOD_keyDown131 = 131;
    private static final int METHOD_keyUp132 = 132;
    private static final int METHOD_action133 = 133;
    private static final int METHOD_gotFocus134 = 134;
    private static final int METHOD_lostFocus135 = 135;
    private static final int METHOD_transferFocus136 = 136;
    private static final int METHOD_nextFocus137 = 137;
    private static final int METHOD_transferFocusUpCycle138 = 138;
    private static final int METHOD_hasFocus139 = 139;
    private static final int METHOD_add140 = 140;
    private static final int METHOD_remove141 = 141;
    private static final int METHOD_toString142 = 142;
    private static final int METHOD_list143 = 143;
    private static final int METHOD_list144 = 144;
    private static final int METHOD_list145 = 145;

    // Method array
    /*lazy MethodDescriptor*/
    private static MethodDescriptor[] getMdescriptor(){
        MethodDescriptor[] methods = new MethodDescriptor[146];

        try {
            methods[METHOD_updateUI0] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("updateUI", new Class[] {}));
            methods[METHOD_updateUI0].setDisplayName ( "" );
            methods[METHOD_update1] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("update", new Class[] {java.awt.Graphics.class}));
            methods[METHOD_update1].setDisplayName ( "" );
            methods[METHOD_paint2] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("paint", new Class[] {java.awt.Graphics.class}));
            methods[METHOD_paint2].setDisplayName ( "" );
            methods[METHOD_printAll3] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("printAll", new Class[] {java.awt.Graphics.class}));
            methods[METHOD_printAll3].setDisplayName ( "" );
            methods[METHOD_print4] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("print", new Class[] {java.awt.Graphics.class}));
            methods[METHOD_print4].setDisplayName ( "" );
            methods[METHOD_requestFocus5] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("requestFocus", new Class[] {}));
            methods[METHOD_requestFocus5].setDisplayName ( "" );
            methods[METHOD_requestFocus6] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("requestFocus", new Class[] {Boolean.TYPE}));
            methods[METHOD_requestFocus6].setDisplayName ( "" );
            methods[METHOD_requestFocusInWindow7] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("requestFocusInWindow", new Class[] {}));
            methods[METHOD_requestFocusInWindow7].setDisplayName ( "" );
            methods[METHOD_grabFocus8] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("grabFocus", new Class[] {}));
            methods[METHOD_grabFocus8].setDisplayName ( "" );
            methods[METHOD_contains9] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("contains", new Class[] {Integer.TYPE, Integer.TYPE}));
            methods[METHOD_contains9].setDisplayName ( "" );
            methods[METHOD_getInsets10] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getInsets", new Class[] {java.awt.Insets.class}));
            methods[METHOD_getInsets10].setDisplayName ( "" );
            methods[METHOD_registerKeyboardAction11] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("registerKeyboardAction", new Class[] {java.awt.event.ActionListener.class, java.lang.String.class, javax.swing.KeyStroke.class, Integer.TYPE}));
            methods[METHOD_registerKeyboardAction11].setDisplayName ( "" );
            methods[METHOD_registerKeyboardAction12] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("registerKeyboardAction", new Class[] {java.awt.event.ActionListener.class, javax.swing.KeyStroke.class, Integer.TYPE}));
            methods[METHOD_registerKeyboardAction12].setDisplayName ( "" );
            methods[METHOD_unregisterKeyboardAction13] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("unregisterKeyboardAction", new Class[] {javax.swing.KeyStroke.class}));
            methods[METHOD_unregisterKeyboardAction13].setDisplayName ( "" );
            methods[METHOD_getConditionForKeyStroke14] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getConditionForKeyStroke", new Class[] {javax.swing.KeyStroke.class}));
            methods[METHOD_getConditionForKeyStroke14].setDisplayName ( "" );
            methods[METHOD_getActionForKeyStroke15] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getActionForKeyStroke", new Class[] {javax.swing.KeyStroke.class}));
            methods[METHOD_getActionForKeyStroke15].setDisplayName ( "" );
            methods[METHOD_resetKeyboardActions16] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("resetKeyboardActions", new Class[] {}));
            methods[METHOD_resetKeyboardActions16].setDisplayName ( "" );
            methods[METHOD_setInputMap17] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("setInputMap", new Class[] {Integer.TYPE, javax.swing.InputMap.class}));
            methods[METHOD_setInputMap17].setDisplayName ( "" );
            methods[METHOD_getInputMap18] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getInputMap", new Class[] {Integer.TYPE}));
            methods[METHOD_getInputMap18].setDisplayName ( "" );
            methods[METHOD_getInputMap19] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getInputMap", new Class[] {}));
            methods[METHOD_getInputMap19].setDisplayName ( "" );
            methods[METHOD_requestDefaultFocus20] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("requestDefaultFocus", new Class[] {}));
            methods[METHOD_requestDefaultFocus20].setDisplayName ( "" );
            methods[METHOD_getDefaultLocale21] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getDefaultLocale", new Class[] {}));
            methods[METHOD_getDefaultLocale21].setDisplayName ( "" );
            methods[METHOD_setDefaultLocale22] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("setDefaultLocale", new Class[] {java.util.Locale.class}));
            methods[METHOD_setDefaultLocale22].setDisplayName ( "" );
            methods[METHOD_getToolTipText23] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getToolTipText", new Class[] {java.awt.event.MouseEvent.class}));
            methods[METHOD_getToolTipText23].setDisplayName ( "" );
            methods[METHOD_getToolTipLocation24] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getToolTipLocation", new Class[] {java.awt.event.MouseEvent.class}));
            methods[METHOD_getToolTipLocation24].setDisplayName ( "" );
            methods[METHOD_createToolTip25] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("createToolTip", new Class[] {}));
            methods[METHOD_createToolTip25].setDisplayName ( "" );
            methods[METHOD_scrollRectToVisible26] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("scrollRectToVisible", new Class[] {java.awt.Rectangle.class}));
            methods[METHOD_scrollRectToVisible26].setDisplayName ( "" );
            methods[METHOD_enable27] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("enable", new Class[] {}));
            methods[METHOD_enable27].setDisplayName ( "" );
            methods[METHOD_disable28] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("disable", new Class[] {}));
            methods[METHOD_disable28].setDisplayName ( "" );
            methods[METHOD_getClientProperty29] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getClientProperty", new Class[] {java.lang.Object.class}));
            methods[METHOD_getClientProperty29].setDisplayName ( "" );
            methods[METHOD_putClientProperty30] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("putClientProperty", new Class[] {java.lang.Object.class, java.lang.Object.class}));
            methods[METHOD_putClientProperty30].setDisplayName ( "" );
            methods[METHOD_isLightweightComponent31] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("isLightweightComponent", new Class[] {java.awt.Component.class}));
            methods[METHOD_isLightweightComponent31].setDisplayName ( "" );
            methods[METHOD_reshape32] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("reshape", new Class[] {Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE}));
            methods[METHOD_reshape32].setDisplayName ( "" );
            methods[METHOD_getBounds33] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getBounds", new Class[] {java.awt.Rectangle.class}));
            methods[METHOD_getBounds33].setDisplayName ( "" );
            methods[METHOD_getSize34] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getSize", new Class[] {java.awt.Dimension.class}));
            methods[METHOD_getSize34].setDisplayName ( "" );
            methods[METHOD_getLocation35] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getLocation", new Class[] {java.awt.Point.class}));
            methods[METHOD_getLocation35].setDisplayName ( "" );
            methods[METHOD_computeVisibleRect36] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("computeVisibleRect", new Class[] {java.awt.Rectangle.class}));
            methods[METHOD_computeVisibleRect36].setDisplayName ( "" );
            methods[METHOD_firePropertyChange37] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("firePropertyChange", new Class[] {java.lang.String.class, Byte.TYPE, Byte.TYPE}));
            methods[METHOD_firePropertyChange37].setDisplayName ( "" );
            methods[METHOD_firePropertyChange38] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("firePropertyChange", new Class[] {java.lang.String.class, Character.TYPE, Character.TYPE}));
            methods[METHOD_firePropertyChange38].setDisplayName ( "" );
            methods[METHOD_firePropertyChange39] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("firePropertyChange", new Class[] {java.lang.String.class, Short.TYPE, Short.TYPE}));
            methods[METHOD_firePropertyChange39].setDisplayName ( "" );
            methods[METHOD_firePropertyChange40] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("firePropertyChange", new Class[] {java.lang.String.class, Integer.TYPE, Integer.TYPE}));
            methods[METHOD_firePropertyChange40].setDisplayName ( "" );
            methods[METHOD_firePropertyChange41] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("firePropertyChange", new Class[] {java.lang.String.class, Long.TYPE, Long.TYPE}));
            methods[METHOD_firePropertyChange41].setDisplayName ( "" );
            methods[METHOD_firePropertyChange42] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("firePropertyChange", new Class[] {java.lang.String.class, Float.TYPE, Float.TYPE}));
            methods[METHOD_firePropertyChange42].setDisplayName ( "" );
            methods[METHOD_firePropertyChange43] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("firePropertyChange", new Class[] {java.lang.String.class, Double.TYPE, Double.TYPE}));
            methods[METHOD_firePropertyChange43].setDisplayName ( "" );
            methods[METHOD_firePropertyChange44] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("firePropertyChange", new Class[] {java.lang.String.class, Boolean.TYPE, Boolean.TYPE}));
            methods[METHOD_firePropertyChange44].setDisplayName ( "" );
            methods[METHOD_addPropertyChangeListener45] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("addPropertyChangeListener", new Class[] {java.lang.String.class, java.beans.PropertyChangeListener.class}));
            methods[METHOD_addPropertyChangeListener45].setDisplayName ( "" );
            methods[METHOD_removePropertyChangeListener46] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("removePropertyChangeListener", new Class[] {java.lang.String.class, java.beans.PropertyChangeListener.class}));
            methods[METHOD_removePropertyChangeListener46].setDisplayName ( "" );
            methods[METHOD_getPropertyChangeListeners47] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getPropertyChangeListeners", new Class[] {java.lang.String.class}));
            methods[METHOD_getPropertyChangeListeners47].setDisplayName ( "" );
            methods[METHOD_getListeners48] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getListeners", new Class[] {java.lang.Class.class}));
            methods[METHOD_getListeners48].setDisplayName ( "" );
            methods[METHOD_addNotify49] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("addNotify", new Class[] {}));
            methods[METHOD_addNotify49].setDisplayName ( "" );
            methods[METHOD_removeNotify50] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("removeNotify", new Class[] {}));
            methods[METHOD_removeNotify50].setDisplayName ( "" );
            methods[METHOD_repaint51] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("repaint", new Class[] {Long.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE}));
            methods[METHOD_repaint51].setDisplayName ( "" );
            methods[METHOD_repaint52] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("repaint", new Class[] {java.awt.Rectangle.class}));
            methods[METHOD_repaint52].setDisplayName ( "" );
            methods[METHOD_revalidate53] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("revalidate", new Class[] {}));
            methods[METHOD_revalidate53].setDisplayName ( "" );
            methods[METHOD_paintImmediately54] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("paintImmediately", new Class[] {Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE}));
            methods[METHOD_paintImmediately54].setDisplayName ( "" );
            methods[METHOD_paintImmediately55] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("paintImmediately", new Class[] {java.awt.Rectangle.class}));
            methods[METHOD_paintImmediately55].setDisplayName ( "" );
            methods[METHOD_countComponents56] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("countComponents", new Class[] {}));
            methods[METHOD_countComponents56].setDisplayName ( "" );
            methods[METHOD_insets57] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("insets", new Class[] {}));
            methods[METHOD_insets57].setDisplayName ( "" );
            methods[METHOD_add58] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("add", new Class[] {java.awt.Component.class}));
            methods[METHOD_add58].setDisplayName ( "" );
            methods[METHOD_add59] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("add", new Class[] {java.lang.String.class, java.awt.Component.class}));
            methods[METHOD_add59].setDisplayName ( "" );
            methods[METHOD_add60] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("add", new Class[] {java.awt.Component.class, Integer.TYPE}));
            methods[METHOD_add60].setDisplayName ( "" );
            methods[METHOD_add61] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("add", new Class[] {java.awt.Component.class, java.lang.Object.class}));
            methods[METHOD_add61].setDisplayName ( "" );
            methods[METHOD_add62] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("add", new Class[] {java.awt.Component.class, java.lang.Object.class, Integer.TYPE}));
            methods[METHOD_add62].setDisplayName ( "" );
            methods[METHOD_remove63] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("remove", new Class[] {Integer.TYPE}));
            methods[METHOD_remove63].setDisplayName ( "" );
            methods[METHOD_remove64] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("remove", new Class[] {java.awt.Component.class}));
            methods[METHOD_remove64].setDisplayName ( "" );
            methods[METHOD_removeAll65] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("removeAll", new Class[] {}));
            methods[METHOD_removeAll65].setDisplayName ( "" );
            methods[METHOD_doLayout66] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("doLayout", new Class[] {}));
            methods[METHOD_doLayout66].setDisplayName ( "" );
            methods[METHOD_layout67] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("layout", new Class[] {}));
            methods[METHOD_layout67].setDisplayName ( "" );
            methods[METHOD_invalidate68] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("invalidate", new Class[] {}));
            methods[METHOD_invalidate68].setDisplayName ( "" );
            methods[METHOD_validate69] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("validate", new Class[] {}));
            methods[METHOD_validate69].setDisplayName ( "" );
            methods[METHOD_preferredSize70] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("preferredSize", new Class[] {}));
            methods[METHOD_preferredSize70].setDisplayName ( "" );
            methods[METHOD_minimumSize71] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("minimumSize", new Class[] {}));
            methods[METHOD_minimumSize71].setDisplayName ( "" );
            methods[METHOD_paintComponents72] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("paintComponents", new Class[] {java.awt.Graphics.class}));
            methods[METHOD_paintComponents72].setDisplayName ( "" );
            methods[METHOD_printComponents73] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("printComponents", new Class[] {java.awt.Graphics.class}));
            methods[METHOD_printComponents73].setDisplayName ( "" );
            methods[METHOD_deliverEvent74] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("deliverEvent", new Class[] {java.awt.Event.class}));
            methods[METHOD_deliverEvent74].setDisplayName ( "" );
            methods[METHOD_getComponentAt75] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getComponentAt", new Class[] {Integer.TYPE, Integer.TYPE}));
            methods[METHOD_getComponentAt75].setDisplayName ( "" );
            methods[METHOD_locate76] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("locate", new Class[] {Integer.TYPE, Integer.TYPE}));
            methods[METHOD_locate76].setDisplayName ( "" );
            methods[METHOD_getComponentAt77] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getComponentAt", new Class[] {java.awt.Point.class}));
            methods[METHOD_getComponentAt77].setDisplayName ( "" );
            methods[METHOD_findComponentAt78] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("findComponentAt", new Class[] {Integer.TYPE, Integer.TYPE}));
            methods[METHOD_findComponentAt78].setDisplayName ( "" );
            methods[METHOD_findComponentAt79] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("findComponentAt", new Class[] {java.awt.Point.class}));
            methods[METHOD_findComponentAt79].setDisplayName ( "" );
            methods[METHOD_isAncestorOf80] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("isAncestorOf", new Class[] {java.awt.Component.class}));
            methods[METHOD_isAncestorOf80].setDisplayName ( "" );
            methods[METHOD_list81] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("list", new Class[] {java.io.PrintStream.class, Integer.TYPE}));
            methods[METHOD_list81].setDisplayName ( "" );
            methods[METHOD_list82] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("list", new Class[] {java.io.PrintWriter.class, Integer.TYPE}));
            methods[METHOD_list82].setDisplayName ( "" );
            methods[METHOD_areFocusTraversalKeysSet83] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("areFocusTraversalKeysSet", new Class[] {Integer.TYPE}));
            methods[METHOD_areFocusTraversalKeysSet83].setDisplayName ( "" );
            methods[METHOD_isFocusCycleRoot84] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("isFocusCycleRoot", new Class[] {java.awt.Container.class}));
            methods[METHOD_isFocusCycleRoot84].setDisplayName ( "" );
            methods[METHOD_transferFocusBackward85] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("transferFocusBackward", new Class[] {}));
            methods[METHOD_transferFocusBackward85].setDisplayName ( "" );
            methods[METHOD_transferFocusDownCycle86] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("transferFocusDownCycle", new Class[] {}));
            methods[METHOD_transferFocusDownCycle86].setDisplayName ( "" );
            methods[METHOD_applyComponentOrientation87] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("applyComponentOrientation", new Class[] {java.awt.ComponentOrientation.class}));
            methods[METHOD_applyComponentOrientation87].setDisplayName ( "" );
            methods[METHOD_enable88] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("enable", new Class[] {Boolean.TYPE}));
            methods[METHOD_enable88].setDisplayName ( "" );
            methods[METHOD_enableInputMethods89] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("enableInputMethods", new Class[] {Boolean.TYPE}));
            methods[METHOD_enableInputMethods89].setDisplayName ( "" );
            methods[METHOD_show90] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("show", new Class[] {}));
            methods[METHOD_show90].setDisplayName ( "" );
            methods[METHOD_show91] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("show", new Class[] {Boolean.TYPE}));
            methods[METHOD_show91].setDisplayName ( "" );
            methods[METHOD_hide92] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("hide", new Class[] {}));
            methods[METHOD_hide92].setDisplayName ( "" );
            methods[METHOD_getLocation93] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getLocation", new Class[] {}));
            methods[METHOD_getLocation93].setDisplayName ( "" );
            methods[METHOD_location94] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("location", new Class[] {}));
            methods[METHOD_location94].setDisplayName ( "" );
            methods[METHOD_setLocation95] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("setLocation", new Class[] {Integer.TYPE, Integer.TYPE}));
            methods[METHOD_setLocation95].setDisplayName ( "" );
            methods[METHOD_move96] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("move", new Class[] {Integer.TYPE, Integer.TYPE}));
            methods[METHOD_move96].setDisplayName ( "" );
            methods[METHOD_setLocation97] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("setLocation", new Class[] {java.awt.Point.class}));
            methods[METHOD_setLocation97].setDisplayName ( "" );
            methods[METHOD_getSize98] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getSize", new Class[] {}));
            methods[METHOD_getSize98].setDisplayName ( "" );
            methods[METHOD_size99] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("size", new Class[] {}));
            methods[METHOD_size99].setDisplayName ( "" );
            methods[METHOD_setSize100] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("setSize", new Class[] {Integer.TYPE, Integer.TYPE}));
            methods[METHOD_setSize100].setDisplayName ( "" );
            methods[METHOD_resize101] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("resize", new Class[] {Integer.TYPE, Integer.TYPE}));
            methods[METHOD_resize101].setDisplayName ( "" );
            methods[METHOD_setSize102] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("setSize", new Class[] {java.awt.Dimension.class}));
            methods[METHOD_setSize102].setDisplayName ( "" );
            methods[METHOD_resize103] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("resize", new Class[] {java.awt.Dimension.class}));
            methods[METHOD_resize103].setDisplayName ( "" );
            methods[METHOD_bounds104] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("bounds", new Class[] {}));
            methods[METHOD_bounds104].setDisplayName ( "" );
            methods[METHOD_setBounds105] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("setBounds", new Class[] {Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE}));
            methods[METHOD_setBounds105].setDisplayName ( "" );
            methods[METHOD_getFontMetrics106] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("getFontMetrics", new Class[] {java.awt.Font.class}));
            methods[METHOD_getFontMetrics106].setDisplayName ( "" );
            methods[METHOD_paintAll107] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("paintAll", new Class[] {java.awt.Graphics.class}));
            methods[METHOD_paintAll107].setDisplayName ( "" );
            methods[METHOD_repaint108] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("repaint", new Class[] {}));
            methods[METHOD_repaint108].setDisplayName ( "" );
            methods[METHOD_repaint109] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("repaint", new Class[] {Long.TYPE}));
            methods[METHOD_repaint109].setDisplayName ( "" );
            methods[METHOD_repaint110] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("repaint", new Class[] {Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE}));
            methods[METHOD_repaint110].setDisplayName ( "" );
            methods[METHOD_imageUpdate111] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("imageUpdate", new Class[] {java.awt.Image.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE}));
            methods[METHOD_imageUpdate111].setDisplayName ( "" );
            methods[METHOD_createImage112] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("createImage", new Class[] {java.awt.image.ImageProducer.class}));
            methods[METHOD_createImage112].setDisplayName ( "" );
            methods[METHOD_createImage113] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("createImage", new Class[] {Integer.TYPE, Integer.TYPE}));
            methods[METHOD_createImage113].setDisplayName ( "" );
            methods[METHOD_createVolatileImage114] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("createVolatileImage", new Class[] {Integer.TYPE, Integer.TYPE}));
            methods[METHOD_createVolatileImage114].setDisplayName ( "" );
            methods[METHOD_createVolatileImage115] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("createVolatileImage", new Class[] {Integer.TYPE, Integer.TYPE, java.awt.ImageCapabilities.class}));
            methods[METHOD_createVolatileImage115].setDisplayName ( "" );
            methods[METHOD_prepareImage116] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("prepareImage", new Class[] {java.awt.Image.class, java.awt.image.ImageObserver.class}));
            methods[METHOD_prepareImage116].setDisplayName ( "" );
            methods[METHOD_prepareImage117] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("prepareImage", new Class[] {java.awt.Image.class, Integer.TYPE, Integer.TYPE, java.awt.image.ImageObserver.class}));
            methods[METHOD_prepareImage117].setDisplayName ( "" );
            methods[METHOD_checkImage118] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("checkImage", new Class[] {java.awt.Image.class, java.awt.image.ImageObserver.class}));
            methods[METHOD_checkImage118].setDisplayName ( "" );
            methods[METHOD_checkImage119] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("checkImage", new Class[] {java.awt.Image.class, Integer.TYPE, Integer.TYPE, java.awt.image.ImageObserver.class}));
            methods[METHOD_checkImage119].setDisplayName ( "" );
            methods[METHOD_inside120] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("inside", new Class[] {Integer.TYPE, Integer.TYPE}));
            methods[METHOD_inside120].setDisplayName ( "" );
            methods[METHOD_contains121] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("contains", new Class[] {java.awt.Point.class}));
            methods[METHOD_contains121].setDisplayName ( "" );
            methods[METHOD_dispatchEvent122] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("dispatchEvent", new Class[] {java.awt.AWTEvent.class}));
            methods[METHOD_dispatchEvent122].setDisplayName ( "" );
            methods[METHOD_postEvent123] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("postEvent", new Class[] {java.awt.Event.class}));
            methods[METHOD_postEvent123].setDisplayName ( "" );
            methods[METHOD_handleEvent124] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("handleEvent", new Class[] {java.awt.Event.class}));
            methods[METHOD_handleEvent124].setDisplayName ( "" );
            methods[METHOD_mouseDown125] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("mouseDown", new Class[] {java.awt.Event.class, Integer.TYPE, Integer.TYPE}));
            methods[METHOD_mouseDown125].setDisplayName ( "" );
            methods[METHOD_mouseDrag126] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("mouseDrag", new Class[] {java.awt.Event.class, Integer.TYPE, Integer.TYPE}));
            methods[METHOD_mouseDrag126].setDisplayName ( "" );
            methods[METHOD_mouseUp127] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("mouseUp", new Class[] {java.awt.Event.class, Integer.TYPE, Integer.TYPE}));
            methods[METHOD_mouseUp127].setDisplayName ( "" );
            methods[METHOD_mouseMove128] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("mouseMove", new Class[] {java.awt.Event.class, Integer.TYPE, Integer.TYPE}));
            methods[METHOD_mouseMove128].setDisplayName ( "" );
            methods[METHOD_mouseEnter129] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("mouseEnter", new Class[] {java.awt.Event.class, Integer.TYPE, Integer.TYPE}));
            methods[METHOD_mouseEnter129].setDisplayName ( "" );
            methods[METHOD_mouseExit130] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("mouseExit", new Class[] {java.awt.Event.class, Integer.TYPE, Integer.TYPE}));
            methods[METHOD_mouseExit130].setDisplayName ( "" );
            methods[METHOD_keyDown131] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("keyDown", new Class[] {java.awt.Event.class, Integer.TYPE}));
            methods[METHOD_keyDown131].setDisplayName ( "" );
            methods[METHOD_keyUp132] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("keyUp", new Class[] {java.awt.Event.class, Integer.TYPE}));
            methods[METHOD_keyUp132].setDisplayName ( "" );
            methods[METHOD_action133] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("action", new Class[] {java.awt.Event.class, java.lang.Object.class}));
            methods[METHOD_action133].setDisplayName ( "" );
            methods[METHOD_gotFocus134] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("gotFocus", new Class[] {java.awt.Event.class, java.lang.Object.class}));
            methods[METHOD_gotFocus134].setDisplayName ( "" );
            methods[METHOD_lostFocus135] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("lostFocus", new Class[] {java.awt.Event.class, java.lang.Object.class}));
            methods[METHOD_lostFocus135].setDisplayName ( "" );
            methods[METHOD_transferFocus136] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("transferFocus", new Class[] {}));
            methods[METHOD_transferFocus136].setDisplayName ( "" );
            methods[METHOD_nextFocus137] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("nextFocus", new Class[] {}));
            methods[METHOD_nextFocus137].setDisplayName ( "" );
            methods[METHOD_transferFocusUpCycle138] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("transferFocusUpCycle", new Class[] {}));
            methods[METHOD_transferFocusUpCycle138].setDisplayName ( "" );
            methods[METHOD_hasFocus139] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("hasFocus", new Class[] {}));
            methods[METHOD_hasFocus139].setDisplayName ( "" );
            methods[METHOD_add140] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("add", new Class[] {java.awt.PopupMenu.class}));
            methods[METHOD_add140].setDisplayName ( "" );
            methods[METHOD_remove141] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("remove", new Class[] {java.awt.MenuComponent.class}));
            methods[METHOD_remove141].setDisplayName ( "" );
            methods[METHOD_toString142] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("toString", new Class[] {}));
            methods[METHOD_toString142].setDisplayName ( "" );
            methods[METHOD_list143] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("list", new Class[] {}));
            methods[METHOD_list143].setDisplayName ( "" );
            methods[METHOD_list144] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("list", new Class[] {java.io.PrintStream.class}));
            methods[METHOD_list144].setDisplayName ( "" );
            methods[METHOD_list145] = new MethodDescriptor ( workbench.gui.profiles.ConnectionEditorPanel.class.getMethod("list", new Class[] {java.io.PrintWriter.class}));
            methods[METHOD_list145].setDisplayName ( "" );
        }
        catch( Exception e) {}//GEN-HEADEREND:Methods

		// Here you can add code for customizing the methods array.

        return methods;         }//GEN-LAST:Methods


    private static final int defaultPropertyIndex = -1;//GEN-BEGIN:Idx
    private static final int defaultEventIndex = -1;//GEN-END:Idx


 //GEN-FIRST:Superclass

	// Here you can add code for customizing the Superclass BeanInfo.

 //GEN-LAST:Superclass

	/**
	 * Gets the bean's <code>BeanDescriptor</code>s.
	 *
	 * @return BeanDescriptor describing the editable
	 * properties of this bean.  May return null if the
	 * information should be obtained by automatic analysis.
	 */
	public BeanDescriptor getBeanDescriptor()
	{
		return getBdescriptor();
	}

	/**
	 * Gets the bean's <code>PropertyDescriptor</code>s.
	 *
	 * @return An array of PropertyDescriptors describing the editable
	 * properties supported by this bean.  May return null if the
	 * information should be obtained by automatic analysis.
	 * <p>
	 * If a property is indexed, then its entry in the result array will
	 * belong to the IndexedPropertyDescriptor subclass of PropertyDescriptor.
	 * A client of getPropertyDescriptors can use "instanceof" to check
	 * if a given PropertyDescriptor is an IndexedPropertyDescriptor.
	 */
	public PropertyDescriptor[] getPropertyDescriptors()
	{
		return getPdescriptor();
	}

	/**
	 * Gets the bean's <code>EventSetDescriptor</code>s.
	 *
	 * @return  An array of EventSetDescriptors describing the kinds of
	 * events fired by this bean.  May return null if the information
	 * should be obtained by automatic analysis.
	 */
	public EventSetDescriptor[] getEventSetDescriptors()
	{
		return getEdescriptor();
	}

	/**
	 * Gets the bean's <code>MethodDescriptor</code>s.
	 *
	 * @return  An array of MethodDescriptors describing the methods
	 * implemented by this bean.  May return null if the information
	 * should be obtained by automatic analysis.
	 */
	public MethodDescriptor[] getMethodDescriptors()
	{
		return getMdescriptor();
	}

	/**
	 * A bean may have a "default" property that is the property that will
	 * mostly commonly be initially chosen for update by human's who are
	 * customizing the bean.
	 * @return  Index of default property in the PropertyDescriptor array
	 * 		returned by getPropertyDescriptors.
	 * <P>	Returns -1 if there is no default property.
	 */
	public int getDefaultPropertyIndex()
	{
		return defaultPropertyIndex;
	}

	/**
	 * A bean may have a "default" event that is the event that will
	 * mostly commonly be used by human's when using the bean.
	 * @return Index of default event in the EventSetDescriptor array
	 *		returned by getEventSetDescriptors.
	 * <P>	Returns -1 if there is no default event.
	 */
	public int getDefaultEventIndex()
	{
		return defaultEventIndex;
	}
}

