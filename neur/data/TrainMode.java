
package neur.data;

public enum TrainMode 
{
        SUPERVISED_BATCH_MODE(true),
        SUPERVISED_ONLINE_MODE(true),
        SUPERVISED_MIXED_BO_MODE(true),
        
        SUPERVISED_NO_TRAINING(true),
        UNSUPERVISED_NO_TRAINING(false),
        ;
        

        private boolean supervised = true;
        public boolean isSupervised() { return supervised; }
        
        private TrainMode(boolean supervised)
        {
            this.supervised = supervised;
        }
}
