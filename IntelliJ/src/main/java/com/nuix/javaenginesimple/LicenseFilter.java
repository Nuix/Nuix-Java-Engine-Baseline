package com.nuix.javaenginesimple;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nuix.engine.AvailableLicence;

/***
 * Specifies minimum requirements you may have when obtaining a license.  Used by {@link EngineWrapper} when iterating available
 * licenses to determine which one to pick from those available.
 * @author Jason Wells
 *
 */
public class LicenseFilter {
	// Obtain a logger instance for this class
	private final static Logger logger = LogManager.getLogger("LicenseFilter");
	
	private int minWorkers = 0;
	private int maxWorkers = 0;
	private String name = null;
	private List<String> requiredFeatures = new ArrayList<String>();

	/***
	 * Creates a new instance which by default will accept any license.
	 */
	public LicenseFilter() {
		acquireAnyLicense();
	}
	
	/***
	 * Resets filtering criteria of this instance such that it will accept any license configuration.
	 */
	public void acquireAnyLicense() {
		minWorkers = 0;
		maxWorkers = 0;
		name = null;
		requiredFeatures.clear();
	}
	
	/***
	 * Checks the configured license requirements of this instance against a given license.  If all requirements
	 * are found to have been met, returns true.  If any requirement is found to have been not met, returns false.
	 * @param license The license to inspect.
	 * @return True if all requirements met, false if any requirement has not been met.
	 */
	public boolean isValid(AvailableLicence license) {
		
		int workerCount = license.getWorkers();
		// Verify the minimum worker count
		if(minWorkers > 0 && workerCount < minWorkers) {
			logger.info(String.format("!!! License has %s workers, filter specifies a minimum of %s",workerCount,minWorkers));
			return false;
		}
		
		// Verify the maximum worker count.  This is intended for situations where perhaps multiple fixed worker count licenses
		// may be available and you don't want to acquire licenses with larger worker counts.  When acquiring from a license server
		// and the license shares a worker pool (canChooseWorkers below) then the maximum is ignored.
		if(license.canChooseWorkers() && maxWorkers > 0 && workerCount > maxWorkers) {
			logger.info(String.format("!!!! License has %s workers, filter specifies a maximum of %s",workerCount,minWorkers));
			return false;
		}
		
		// Verify short name
		String shortName = license.getShortName().toLowerCase();
		if(name != null && shortName.contentEquals(name) == false) return false;
		
		// Verify presence of required features
		if(requiredFeatures != null && requiredFeatures.size() > 0) {
			for(String featureName : requiredFeatures) {
				if(license.hasFeature(featureName) == false) {
					logger.info(String.format("!!! License lacks required feature '%s'", featureName));
					return false;
				}
			}
		}
		
		// If we have reached here, none of the conditions above failed
		// this license and therefore we give it out stamp of approval.
		return true;
	}

	/***
	 * Gets the minimum number of workers that must be present in a license
	 * for this filter to approve it.  Note that when a given license allows you to select the number of
	 * workers to acquire from a pool of workers, this value will be the number of workers requested from the pool.
	 * @return The minimum number of workers that must be present in a license.  A value of 0 means no minimum.
	 */
	public int getMinWorkers() {
		return minWorkers;
	}

	/***
	 * Sets the minimum number of workers that must be present in a license
	 * for this filter to approve it.  Note that when a given license allows you to select the number of
	 * workers to acquire from a pool of workers, this value will be the number of workers requested from the pool.
	 * @param minWorkers The minimum number of workers that must be present in a license.  A value of 0 mean no minimum.
	 */
	public void setMinWorkers(int minWorkers) {
		this.minWorkers = minWorkers;
	}

	/***
	 * Gets the maximum number of workers that can be present in a license
	 * for this filter to approve it.  Note that this filtering criteria is only considered
	 * when a given license has a fixed number of workers associated.
	 * @return The maximum number of workers that can be present in a license.  A value of 0 means no maximum.
	 */
	public int getMaxWorkers() {
		return maxWorkers;
	}

	/***
	 * Sets the maximum number of workers that can be present in a license
	 * for this filter to approve it.  Note that this filtering criteria is only considered
	 * when a given license has a fixed number of workers associated.
	 * @param maxWorkers The maximum number of workers that can be present in a license.  A value of 0 means no maximum.
	 */
	public void setMaxWorkers(int maxWorkers) {
		this.maxWorkers = maxWorkers;
	}

	/***
	 * Gets the required short name (such as "enterprise-workstation") that a license must have
	 * for this filter to approve it.
	 * @return The required short name a license must have to be approved by this filter.  A value of null means this requirement is ignored.
	 */
	public String getShortName() {
		return name;
	}

	/***
	 * Sets the required short name (such as "enterprise-workstation") that a license must have
	 * for this filter to approve it.
	 * @param name The required short name a license must have to be approved by this filter.  A value of null means this requirement is ignored.
	 */
	public void setShortName(String name) {
		this.name = name.toLowerCase();
	}

	/***
	 * Gets the list of required features a license must have for this filter to approve it.
	 * @return List of required features
	 */
	public List<String> getRequiredFeatures() {
		return requiredFeatures;
	}

	/***
	 * Sets the list of required features a license must have for this filter to approve it.
	 * @param requiredFeatures List of required feature names.  Providing an empty list or null means this requirement is ignored.
	 */
	public void setRequiredFeatures(List<String> requiredFeatures) {
		this.requiredFeatures = requiredFeatures;
	}
	
	/***
	 * Adds a specified feature to the list of required features a license must have for this filter to approve it.
	 * @param featureName The name of a required license feature.
	 */
	public void addRequiredFeature(String featureName) {
		if(requiredFeatures == null) {
			requiredFeatures = new ArrayList<String>();
		}
		requiredFeatures.add(featureName);
	}
	
	/***
	 * Removes all features from the list of required features a license must have for this filter to approve it.  This effectively is
	 * telling this instance not to filter a license based on what features are present.
	 */
	public void clearRequiredFeatures() {
		if(requiredFeatures == null) {
			requiredFeatures = new ArrayList<String>();
		}
		requiredFeatures.clear();
	}

	@Override
	public String toString() {
		return "LicenseFilter [minWorkers=" + minWorkers + ", maxWorkers=" + maxWorkers + ", name=" + name
				+ ", requiredFeatures=" + requiredFeatures + "]";
	}
}
