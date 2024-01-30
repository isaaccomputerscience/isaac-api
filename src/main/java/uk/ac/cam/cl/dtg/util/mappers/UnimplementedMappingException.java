package uk.ac.cam.cl.dtg.util.mappers;

public class UnimplementedMappingException extends RuntimeException {
  public UnimplementedMappingException() {
    super();
  }

  public UnimplementedMappingException(String message) {
    super(message);
  }

  public UnimplementedMappingException(Class<?> sourceClass, Class<?> targetClass) {
    super(String.format("Invocation of unimplemented mapping from %s to %s", sourceClass.getSimpleName(),
        targetClass.getSimpleName()));
  }
}
