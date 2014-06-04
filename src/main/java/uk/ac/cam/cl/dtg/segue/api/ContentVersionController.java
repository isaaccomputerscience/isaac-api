package uk.ac.cam.cl.dtg.segue.api;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.dao.IContentManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

// TODO: convert this class into a singleton instead of using static fields.
public class ContentVersionController {
	
	private static final Logger log = LoggerFactory.getLogger(ContentVersionController.class);
	
	private static volatile String liveVersion;
	
	private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
	
	private PropertiesLoader properties;
	
	private IContentManager contentManager;
	
	@Inject
	public ContentVersionController(PropertiesLoader properties, IContentManager contentManager){
		this.properties = properties;
		this.contentManager = contentManager;
		
		// we want to make sure we have set a default liveVersion number
		if(null == liveVersion){
			liveVersion = this.properties.getProperty(Constants.INITIAL_LIVE_VERSION);		
			log.info("Setting live version of the site from properties file to " + liveVersion);
		}
	}
	
	/**
	 * Gets the current live version of the Segue content as far as the controller is concerned.
	 * 
	 * This method is threadsafe.
	 * 
	 * @return a version id
	 */
	public String getLiveVersion(){
		synchronized(liveVersion){
			return liveVersion;
		}
	}

	/**
	 * Trigger a sync job that will request a sync and subsequent index of the latest version of the content available 
	 * 
	 * @return a future containing a string representation of the version that is available.
	 */
	public Future<String> triggerSyncJob(){
		return this.triggerSyncJob(null);
	}
	
	/**
	 * Trigger a sync job that will request a sync and subsequent index of a specific version of the content.
	 * 
	 * @param version to sync
	 * @return a future containing a string representation of the version that is available.
	 */
	public Future<String> triggerSyncJob(String version){
		return executorService.submit(new ContentSynchronisationWorker(this, version));
	}

	/**
	 * This method is intended to be used by Synchronisation jobs to inform the controller that they have completed their work.
	 * @param the version that has just been indexed. 
	 */
	public synchronized void syncJobCompleteCallback(String version, boolean success){
		// for use by ContentSynchronisationWorkers to alert the controller that they have finished
		if(!success){
			log.error("ContentSynchronisationWorker reported a failure to synchronise");
			return;
		}
		
		// verify that the version is indeed cached
		if(!contentManager.getCachedVersionList().contains(version)){
			// if not just return without doing anything.
			log.error("Sync job informed version controller that a version was ready and it lied. The version is no longer cached. Terminating sync job.");
			return;
		}
		
		// Decide if we have to update the live version or not.
		if(Boolean.parseBoolean(properties.getProperty(Constants.FOLLOW_GIT_VERSION))){

			// acquire the lock for an atomic update
			synchronized(liveVersion){
				// set it to the live version only if it is newer than the current live version.
				if(contentManager.compareTo(version, this.getLiveVersion()) > 0){
					this.setLiveVersion(version);
				}
				else{
					log.info("Not changing live version as the version indexed is older than the new one.");
				}
			}
		}
		else{
			// we don't want to change the latest version until told to do so.
			log.info("New content version " + version + " indexed and available. Not changing liveVersion of the site as per configuration instruction.");
		}

		if(success)
			this.cleanupCache(version);
		log.info("Sync job completed - callback received and finished. ");
	}
	
	/**
	 * Change the version that the controller considers to be the live version.
	 * 
	 * This method is threadsafe.
	 * 
	 * @param newLiveVersion
	 */
	public void setLiveVersion(String newLiveVersion){
		synchronized(liveVersion){
			log.info("Changing live version from " + this.getLiveVersion() + " to " + newLiveVersion);
			liveVersion = newLiveVersion;	
		}
	}
	
	public IContentManager getContentManager(){
		return contentManager;
	}
	
	/**
	 * Check to see if the the version specified is in use by the controller for some reason
	 * 
	 * @param version
	 * @return true if it is being used, false if not.
	 */
	public boolean isVersionInUse(String version){
		// TODO: this method will be used to indicate if a version is currently being used in A/B testing in the future. For now it is just checking if it is the live one.
		return getLiveVersion().equals(version);
	}
	
	/**
	 * This method should use the configuration settings to maintain the cache of the content manager object.
	 */
	public synchronized void cleanupCache(String versionJustIndexed){
		int max_cache_size = Integer.parseInt(properties.getProperty(Constants.MAX_VERSIONS_TO_CACHE));
		
		// first check if our cache is bigger than we want it to be
		if(contentManager.getCachedVersionList().size() > max_cache_size){
			log.info("Cache is full (" + contentManager.getCachedVersionList().size() + ") finding and deleting old versions");
			// Now we want to decide which versions we can safely get rid of.
			List<String> allVersions = contentManager.listAvailableVersions();
			
			// got through all versions in reverse until you find the oldest one that is also in the cached versions list and then remove it.
			for(int index = allVersions.size()-1; contentManager.getCachedVersionList().size() > max_cache_size && index >= 0; index--){

				// check if the version is cached
				if(contentManager.getCachedVersionList().contains(allVersions.get(index))){
					// check we are not deleting the version that is currently in use before we delete it.
					if(!isVersionInUse(allVersions.get(index)) && !versionJustIndexed.equals(allVersions.get(index))){
						log.info("Requesting to delete the content at version " + allVersions.get(index) + " from the cache.");
						contentManager.clearCache(allVersions.get(index));						
					}
				}
			}

			// we couldn't free up enough space
			if(contentManager.getCachedVersionList().size() > max_cache_size){
				log.warn("Warning unable to reduce cache to target size: current cache size is " + contentManager.getCachedVersionList().size());
			}
		}
		else
		{
			log.info("Not evicting cache as we have enough space.");
		}
	}

	/**
	 *  
	 */
	public synchronized void deleteAllCacheData(){	
		log.info("Clearing all caches...");
		contentManager.clearCache();
	}
}
