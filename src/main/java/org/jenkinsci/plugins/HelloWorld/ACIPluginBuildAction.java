/*
 * The MIT License
 *
 * Copyright (c) 2012, Thomas Deruyter, Raynald Briand
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.HelloWorld;

import hudson.model.Action;
import hudson.model.AbstractBuild;

import org.jenkinsci.plugins.HelloWorld.report.Report;
import org.jenkinsci.plugins.HelloWorld.report.Section;
import org.jenkinsci.plugins.HelloWorld.report.Table;
import org.jenkinsci.plugins.HelloWorld.report.Td;
import org.jenkinsci.plugins.HelloWorld.report.Tr;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.export.Exported;
import org.xml.sax.SAXException;

/**
 * Class describing action performed on build page.
 */
public class ACIPluginBuildAction implements Action,
				Serializable, StaplerProxy {
	private static final Logger LOGGER = Logger.getLogger(ACIPluginBuildAction.class.getName());
	/**
	 * Id of the class.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * URL to access data.
	 */
	public static final String URL_NAME = "aciResult";
	private AbstractBuild<?, ?> build;
	private String result;
	private static String command;
	private Report report;
	private ArrayList<ArrayList<String>> fileError;

	/**
	 * Constructor.
	 *
	 * @param build
	 *            The current build
	 * @param command 
	 * @param files
	 *            The current files
	 * @throws InterruptedException
	 *             Interruption
	 * @throws ParserConfigurationException
	 *             Exception in parser configuration
	 * @throws SAXException
	 *             Exception in XML parser
	 * @throws URISyntaxException
	 *             Exception in URL
	 * @throws IOException
	 *             Exception with I/Os
	 */
	public ACIPluginBuildAction(final AbstractBuild<?, ?> build,
			final String logs, String command) throws InterruptedException,
			ParserConfigurationException, SAXException, URISyntaxException,
			IOException {
		this.build = build;
		this.report = new Report();
		this.fileError = new ArrayList<ArrayList<String>>();
		ACIPluginBuildAction.command = command;
		String lspt = System.getProperty("line.separator");
		
//		String logsTmp = "$ git status"+lspt+
//		       "On branch master"+lspt+
//		       "Your branch is up-to-date with 'origin/master'."+lspt+
//               ""+lspt+
//		       "nothing to commit, working directory clean"+lspt;
//		String logsTmp = "$ git status"+lspt+
//		"On branch master"+lspt+
//		"Your branch is up-to-date with 'origin/master'."+lspt+
//		""+lspt+
//		"Changes not staged for commit:"+lspt+
//		"  (use \"git add <file>...\" to update what will be committed)"+lspt+
//		"  (use \"git checkout -- <file>...\" to discard changes in working directory)"+lspt+
//		""+lspt+
//		"        modified:   pom.xml"+lspt+
//		"        modified:   src/main/java/org/jenkinsci/plugins/BuildLogAnnotatorFactory.java"+lspt+
//		""+lspt+
//		"Untracked files:"+lspt+
//		"  (use \"git add <file>...\" to include in what will be committed)"+lspt+
//		""+lspt+
//		"        .classpath"+lspt+
//		"        .project"+lspt+
//		"        .settings/"+lspt+
//		"        target/"+lspt+
//		"        work/"+lspt+
//		""+lspt+
//		"no changes added to commit (use \"git add\" and/or \"git commit -a\")"+lspt;

		LOGGER.log(Level.INFO, "ramon:logs:"+logs);
		LOGGER.log(Level.INFO, "ramon:lspt:"+lspt);
		
        //擷取git status的訊息
		boolean flag = false;
        ArrayList<String> mdfLogsList = new ArrayList<String>();        
        if (logs != null) {
        	String[] lines = logs.split(lspt);
        	for (String line : lines) {
        		if (!flag) {
        			flag = (line.indexOf("On branch master") > -1);
        		}        		
        		if (flag) {
        			mdfLogsList.add(line);
        			flag = !(line.indexOf("nothing to commit,") > -1
        					|| line.indexOf("no changes added to commit") > -1);
        		}
        	}
        }

        Table tmpTable = new Table();
        Tr tmpTr = new Tr();
        Td tmpTd = new Td();
        if (mdfLogsList.size() > 0) {
            for (String md : mdfLogsList) {            	
                tmpTd.setCdata(md + lspt);
                //tmpTd.setTdValue(md + lspt);
            }
            tmpTr.addTd(tmpTd);
            tmpTable.addTr(tmpTr);
        } else {
            tmpTd.setCdata("本次無異動");
            tmpTr.addTd(tmpTd);
            tmpTable.addTr(tmpTr);
        }

	    Section sct = new Section();
	    sct.setSectionName("異動內容");
	    sct.addObject(tmpTable);
		this.report.addSection(sct);
		
		LOGGER.log(Level.INFO, "ramon:path:");
	}

	/**
	 * Get Report.
	 */
	public Report getReport() {
		return report;
	}

	/**
	 * Get Error File.
	 */
	public ArrayList<ArrayList<String>> getFileError() {
		return fileError;
	}

	/**
	 * Get Summary.
	 */
	@Exported
	public String getSummary() {
		StringBuilder builder = new StringBuilder();
		return builder.toString();
		//return "QQQQQQQQQQQQQQQQQQQQQQQQ";
	}

	/**
	 * Get Details.
	 */
	public String getDetails() {
		StringBuilder builder = new StringBuilder();
		return builder.toString();
	}

	/**
	 * Get Result.
	 */
	public String getResult() {
		return this.result;
	}

	/**
	 * Get current build.
	 */
	AbstractBuild<?, ?> getBuild() {
		return this.build;
	}

	/**
	 * Get Target.
	 */
	public Object getTarget() {
		return this.result;
	}

	/**
	 * Get Previous result.
	 */
	String getPreviousResult() {
		ACIPluginBuildAction previousAction = this.getPreviousAction();
		String previousResult = null;
		if (previousAction != null) {
			previousResult = previousAction.getResult();
		}
		return previousResult;
	}

	/**
	 * Get Previous action.
	 */
	ACIPluginBuildAction getPreviousAction() {
		AbstractBuild<?, ?> previousBuild = this.build.getPreviousBuild();
		if (previousBuild != null) {
			return previousBuild.getAction(ACIPluginBuildAction.class);
		}
		return null;
	}

	/**
	 * Get Project Name.
	 */
	public String getProjectName() {
		String str = build.getParent().getName();
		str = str.replace(".", "dot");
		return str;
	}

	/**
	 * Get Build Number.
	 */	
	public int getBuildNumber() {
		return build.getNumber();
	}

	/**
	 * The three functions getIconFileName,getDisplayName,getUrlName create a
	 * link to a new page with url : http://{root}/job/{job name}/URL_NAME for
	 * the page of the build.
	 */	
	public String getIconFileName() {
		// return "/plugin/summary_report/icons/summary_report.png";
		return null;
	}

	/**
	 * The three functions getIconFileName,getDisplayName,getUrlName create a
	 * link to a new page with url : http://{root}/job/{job name}/URL_NAME for
	 * the page of the build.
	 */	
	public String getDisplayName() {
		// return "ACIAction";
		return null;
	}

	/**
	 * The three functions getIconFileName,getDisplayName,getUrlName create a
	 * link to a new page with url : http://{root}/job/{job name}/URL_NAME for
	 * the page of the build.
	 */
	public String getUrlName() {
		if (ACIPluginBuildAction.command != null) {
			String lspt = System.getProperty("line.separator");
			String[] lines = ACIPluginBuildAction.command.split(lspt);
			//echo bli-test.zip
			String t = lines[0].replaceAll("echo", "");
			t = t.replaceAll(".zip", "");
			t = t.trim();
			return t;
		} else {
			return "WWW";
		}
		
		// return URL_NAME;
		//return null;
	}
}
