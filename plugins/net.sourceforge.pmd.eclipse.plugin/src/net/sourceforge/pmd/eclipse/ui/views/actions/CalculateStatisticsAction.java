package net.sourceforge.pmd.eclipse.ui.views.actions;

import java.lang.reflect.InvocationTargetException;

import net.sourceforge.pmd.eclipse.ui.PMDUiConstants;
import net.sourceforge.pmd.eclipse.plugin.PMDPlugin;
import net.sourceforge.pmd.eclipse.ui.model.FileRecord;
import net.sourceforge.pmd.eclipse.ui.model.AbstractPMDRecord;
import net.sourceforge.pmd.eclipse.ui.model.PackageRecord;
import net.sourceforge.pmd.eclipse.ui.nls.StringKeys;
import net.sourceforge.pmd.eclipse.ui.views.ViolationOverview;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;

public class CalculateStatisticsAction extends Action {
    public ViolationOverview violationView;
    
    public CalculateStatisticsAction(ViolationOverview view) {
        super();
        violationView = view;

        // set Image and Tooltip-Text
        setImageDescriptor(PMDPlugin.getImageDescriptor(PMDUiConstants.ICON_BUTTON_CALCULATE));
        setToolTipText(PMDPlugin.getDefault().getStringTable().getString(StringKeys.MSGKEY_VIEW_TOOLTIP_CALCULATE_STATS));
    }
    
    /**
     * Executes the Action
     */
    public void run() {
        try {
            final ProgressMonitorDialog dialog = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
            dialog.setCancelable(true);

            dialog.run(false, false, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    final TreeItem[] items = violationView.getViewer().getTree().getItems();
                    final int toWork = calculateWorkUnits(items);
                    monitor.beginTask(getString(StringKeys.MSGKEY_MONITOR_CALC_STATS_TASK), toWork);
                    for (int i=0; i<items.length; i++) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        if (items[i].getData() instanceof PackageRecord) {
                            final PackageRecord record = (PackageRecord) items[i].getData();
                            final AbstractPMDRecord[] children = record.getChildren();
                            monitor.subTask(getString(StringKeys.MSGKEY_MONITOR_CALC_STATS_OF_PACKAGE) + ": " + record.getName());
                            for (int j=0; j<children.length; j++) {
                                if (children[j] instanceof FileRecord) {
                                    calculateFileRecord((FileRecord)children[j]);
                                    monitor.worked(1);
                                }
                            }
                        } else if (items[i].getData() instanceof FileRecord) {
                            calculateFileRecord((FileRecord) items[i].getData());
                            monitor.worked(1);                            
                        }
                    }
                    
                    violationView.getViewer().refresh(true);
                }
            });
        } catch (InvocationTargetException e) {
            PMDPlugin.getDefault().logError(getString(StringKeys.MSGKEY_ERROR_INVOCATIONTARGET_EXCEPTION), e);
        } catch (InterruptedException e) {
            PMDPlugin.getDefault().logError(getString(StringKeys.MSGKEY_ERROR_INTERRUPTED_EXCEPTION), e);
        }
    }
    
    private void calculateFileRecord(FileRecord fileRecord) {
        if (fileRecord.getLOC() == 0) {
            fileRecord.calculateLinesOfCode();
            fileRecord.calculateNumberOfMethods();
        }
    }

    private int calculateWorkUnits(TreeItem[] items) {
        int toWork = 0;
        for (int i=0; i<items.length; i++) {
            if (items[i].getData() instanceof PackageRecord) {
                final PackageRecord record = (PackageRecord) items[i].getData();
                final AbstractPMDRecord[] children = record.getChildren();
                toWork+=children.length;
            } else if (items[i].getData() instanceof FileRecord) {
                toWork++;
            }
        }
        return toWork;
    }

    /**
     * Helper method to return an NLS string from its key
     */
    private String getString(String key) {
        return PMDPlugin.getDefault().getStringTable().getString(key);
    }
}
