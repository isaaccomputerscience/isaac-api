package uk.ac.cam.cl.dtg.segue.etl;

@FunctionalInterface
public interface CheckedOperation {
  void execute() throws Exception;
}