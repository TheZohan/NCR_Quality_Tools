package tfsReports;

import java.io.File;
import java.io.IOException;

import tfsAccess.TFSProgressCallBack;

public interface TFSReportsGenerator {
	public void ReportGenerator (File allBugsQuery, File coreBugsQuery, File userStoryQuery, File output, TFSProgressCallBack callback);

public void runReports () throws IOException, tfsAccess.TFSException;
}