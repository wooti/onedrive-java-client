package com.wouterbreukink.onedrive;

import java.io.File;
import static com.wouterbreukink.onedrive.CommandLineOpts.getCommandLineOpts;

public class Utils 
{
	public static String getRelativePath(File parent, File children)
	{
		return parent.toURI().relativize(children.toURI()).getPath();
	}
	
	public static String getLocalRelativePath(File children)
	{
		return new File(getCommandLineOpts().getLocalPath()).toURI().relativize(children.toURI()).getPath();
	}
}
