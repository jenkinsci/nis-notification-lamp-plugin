package jenkins.plugins;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class NISConnection extends Builder {

	/**
	 * View URL
	 */
	private final URL		viewUrl;
	
	/**
	 * target server address
	 */
	private final String	address;
	
	/**
	 * target server port
	 */
    private final int		port;
    
    /**
     * Percentage of tasks, which should be accomplished
     */
    private final int    	prozent;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public NISConnection(URL viewUrl, String address, int port, int prozent) {
    	this.viewUrl = viewUrl;
    	this.address = address;
    	this.port = port;
    	this.prozent = prozent;
    }

    public URL getViewUrl() {
        return viewUrl;
    }
    public String getAddress() {
    	return address;
    }
    public int getPort() {
    	return port;
    }
    public int getProzent(){
    	return prozent;
    }
    
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    		java.util.Random random = new java.util.Random();
    		int rand = random.nextInt(1000000);
		
    		URL jenkinsUrl = null;
			try {
				jenkinsUrl = new URL(viewUrl + "?" + rand);
				listener.getLogger().println("URL: " + jenkinsUrl.toString());
			} catch (MalformedURLException e1) {
				System.err.println(e1);
			}
    		String msg = null;
    		
            listener.getLogger().println("Testing .....");
            Ampel ampel = new Ampel(jenkinsUrl);
            UDPSend udpSend = new UDPSend();
            
            int qs = ampel.crawlAndTest(jenkinsUrl, listener);
            
            if(Math.max(qs, prozent) == qs){
            	listener.getLogger().println("*** "+ qs+"% successful.");
            	msg = "SUCCESS";
            }else{
            	listener.getLogger().println("*** less than "+ prozent+"% successful.");
            	msg = "FAILURE";
            }      	
        	try {
				udpSend.sendResult(build.getFullDisplayName(), msg, InetAddress.getByName(address), port);
				listener.getLogger().println("String '" + msg + "' was sent to the server: " + address);
			} catch (UnknownHostException e) {
				listener.getLogger().println(e.toString());
			} catch (IOException e) {
				listener.getLogger().println(e);
			}
        	if(msg.contains("SUCCESS")){
        		return true;
        	}else {
				return false;
			}
        
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    	
    	public FormValidation doCheckViewUrl(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please insert a URL");
            return FormValidation.ok();
        }
    	
    	public FormValidation doCheckAddress(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please insert a host name (of target server) ");
            return FormValidation.ok();
        }
    	
    	public FormValidation doCheckPort(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please insert a port (of target server) ");
            if (value.length() < 2)
                return FormValidation.warning("Isn't the port number too short?");
            return FormValidation.ok();
        }
    	
    	public FormValidation doCheckProzent(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please insert a percentage of tasks, which should be accomplished in order to display the status of the built successful");
            if (value.length() < 2)
                return FormValidation.error("Isn't the percentage number too short?");
            if (value.contains("%"))
            	return FormValidation.error("Please insert only integer");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable viewUrl is used in the configuration screen.
         */
        public String getDisplayName() {
            return "NIS notification lamp";
        }
    }
}