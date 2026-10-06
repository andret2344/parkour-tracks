package eu.andret.parkourtracks.track;

public enum TrackType {
	/**
	 * The basic track.
	 */
	SERVER,
	/**
	 * No timer, no results, no medals and no completion reward.
	 */
	TRAINING,
	/**
	 * Built by players, who are set as its authors by an admin.
	 */
	PLAYERS
}
