package workbench.gui.actions;

/**
 * 	Register actions with the ShortcutManager that are not created upon startup.
 * 	For this, a dummy action is created (with no client) which will
 * 	kick off the registration with the ShortcutManager
 */
public class ActionRegistration
{
	public static void registerActions()
	{
		ToggleTableSourceAction action = new ToggleTableSourceAction(null);
	}
}