/*
 * The MIT License
 * 
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import hudson.FilePath;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.Action;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Messages;
import net.sf.json.JSONObject;

import org.jenkinsci.plugins.HelloWorld.ACIPluginProjectAction;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.Project;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.xml.sax.SAXException;

/**
 * Executes commands by using Windows batch file.
 *
 * @author Kohsuke Kawaguchi
 */
public class MyBatchFile extends CommandInterpreter {
	private static final Logger LOGGER = Logger.getLogger(MyBatchFile.class.getName());
	
    @DataBoundConstructor
    public MyBatchFile(String command) {
        super(command);
    }

    public String[] buildCommandLine(FilePath script) {
        return new String[] {"cmd","/c","call",script.getRemote()};
    }

    protected String getContents() {
        return command+"\r\nexit %ERRORLEVEL%";
    }

    protected String getFileExtension() {
        return ".bat";
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public String getHelpFile() {
            return "/help/project-config/batch.html";
        }

        public String getDisplayName() {
            return "顯示異動內容";
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject data) {
            return new MyBatchFile(data.getString("command"));
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
    
    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
    	boolean rtn = perform(build,launcher,(TaskListener)listener);
    	listener.getLogger().println("ramon:build.getId:" + build.getId());
    	listener.getLogger().println("ramon:build.getNumber:" + build.getNumber());
    	try {
			listener.getLogger().println("ramon:build.getLog:" + build.getLog());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try {
			ACIPluginBuildAction buildAction;
			buildAction = new ACIPluginBuildAction(build, build.getLog(), command);
			build.addAction(buildAction);
		} catch (ParserConfigurationException ex) {
			listener.getLogger().println(ex.toString());
            return false;
		} catch (SAXException ex) {
			listener.getLogger().println(ex.toString());
            return false;
		} catch (URISyntaxException ex) {
			listener.getLogger().println(ex.toString());
            return false;
		} catch (IOException e) {
			listener.getLogger().println(e.toString());
			return false;
		}
		return rtn;
	
    }
   
//	/**
//	 * Get the action associated to the MyBatchFile.
//	 * @param project
//	 * 		Project on which to apply publication
//	 */
//	@Override
//	public Action getProjectAction(final AbstractProject<?, ?> project) {
//		LOGGER.log(Level.INFO, "ramon:getProjectAction:" + command);
//		return new ACIPluginProjectAction(project, command);
//	}
}
