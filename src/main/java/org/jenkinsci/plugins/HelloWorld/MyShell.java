/*
 * The MIT License
 * 
 * Copyright (c) 2004-2011, Sun Microsystems, Inc., Kohsuke Kawaguchi, Jene Jasper, Yahoo! Inc., Seiji Sogabe
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

import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Messages;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.URISyntaxException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import org.xml.sax.SAXException;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Executes a series of commands by using a shell.
 *
 * @author Kohsuke Kawaguchi
 */
public class MyShell extends CommandInterpreter {
    private static final Logger LOGGER = Logger.getLogger(MyShell.class.getName());
	
    @DataBoundConstructor
    public MyShell(String command) {
        super(fixCrLf(command));
    }

    /**
     * Fix CR/LF and always make it Unix style.
     */
    private static String fixCrLf(String s) {
        // eliminate CR
        int idx;
        while((idx=s.indexOf("\r\n"))!=-1)
            s = s.substring(0,idx)+s.substring(idx+1);

        //// add CR back if this is for Windows
        //if(isWindows()) {
        //    idx=0;
        //    while(true) {
        //        idx = s.indexOf('\n',idx);
        //        if(idx==-1) break;
        //        s = s.substring(0,idx)+'\r'+s.substring(idx);
        //        idx+=2;
        //    }
        //}
        return s;
    }

    /**
     * Older versions of bash have a bug where non-ASCII on the first line
     * makes the shell think the file is a binary file and not a script. Adding
     * a leading line feed works around this problem.
     */
    private static String addCrForNonASCII(String s) {
        if(!s.startsWith("#!")) {
            if (s.indexOf('\n')!=0) {
                return "\n" + s;
            }
        }

        return s;
    }

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
        boolean rtn = perform(build,launcher,(TaskListener)listener);
        
        listener.getLogger().println("ramon shell:build.getId:" + build.getId());
    	listener.getLogger().println("ramon shell:build.getNumber:" + build.getNumber());
    	try {
			listener.getLogger().println("ramon shell:build.getLog:" + build.getLog());
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
    
    public String[] buildCommandLine(FilePath script) {
        if(command.startsWith("#!")) {
            // interpreter override
            int end = command.indexOf('\n');
            if(end<0)   end=command.length();
            List<String> args = new ArrayList<String>();
            args.addAll(Arrays.asList(Util.tokenize(command.substring(0,end).trim())));
            args.add(script.getRemote());
            args.set(0,args.get(0).substring(2));   // trim off "#!"
            return args.toArray(new String[args.size()]);
        } else 
            return new String[] { getDescriptor().getShellOrDefault(script.getChannel()), "-xe", script.getRemote()};
    }

    protected String getContents() {
        return addCrForNonASCII(fixCrLf(command));
    }

    protected String getFileExtension() {
        return ".sh";
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * Shell executable, or null to default.
         */
        private String shell;

        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public String getShell() {
            return shell;
        }

        /**
         *  @deprecated 1.403
         *      Use {@link #getShellOrDefault(hudson.remoting.VirtualChannel) }.
         */
        public String getShellOrDefault() {
            if(shell==null)
                return Functions.isWindows() ?"sh":"/bin/sh";
            return shell;
        }

        public String getShellOrDefault(VirtualChannel channel) {
            if (shell != null) 
                return shell;

            String interpreter = null;
            try {
                interpreter = channel.call(new Shellinterpreter());
            } catch (IOException e) {
                LOGGER.warning(e.getMessage());
            } catch (InterruptedException e) {
                LOGGER.warning(e.getMessage());
            }
            if (interpreter == null) {
                interpreter = getShellOrDefault();
            }

            return interpreter;
        }
        
        public void setShell(String shell) {
            this.shell = Util.fixEmptyAndTrim(shell);
            save();
        }

        public String getDisplayName() {
            //return Messages.Shell_DisplayName();
        	return "建置後顯示異動內容";
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject data) {
            return new MyShell(data.getString("command"));
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject data) {
            setShell(req.getParameter("shell"));
            return true;
        }

        /**
         * Check the existence of sh in the given location.
         */
        public FormValidation doCheck(@QueryParameter String value) {
            // Executable requires admin permission
            return FormValidation.validateExecutable(value); 
        }
        
        private static final class Shellinterpreter implements Callable<String, IOException> {

            private static final long serialVersionUID = 1L;

            public String call() throws IOException {
                return Functions.isWindows() ? "sh" : "/bin/sh";
            }
        }
        
    }
    
//	/**
//	 * Get the action associated to the MyBatchFile.
//	 * @param project
//	 * 		Project on which to apply publication
//	 */
//	@Override
//	public Action getProjectAction(final AbstractProject<?, ?> project) {
//		LOGGER.log(Level.INFO, "ramon shell:getProjectAction:" + command);
//		return new ACIPluginProjectAction(project, command);
//	}
}
