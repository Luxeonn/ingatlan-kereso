package ingatlanok.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Pageable that preserves arbitrary offset and limit query parameters. */
public record OffsetPageRequest(long offset, int pageSize, Sort sort) implements Pageable {

  public OffsetPageRequest {
    if (offset < 0 || pageSize < 1) {
      throw new IllegalArgumentException("Offset must be non-negative and page size must be positive");
    }
    sort = sort == null ? Sort.unsorted() : sort;
  }

  @Override
  public int getPageNumber() {
    return (int) (offset / pageSize);
  }

  @Override
  public int getPageSize() {
    return pageSize;
  }

  @Override
  public long getOffset() {
    return offset;
  }

  @Override
  public Sort getSort() {
    return sort;
  }

  @Override
  public Pageable next() {
    return new OffsetPageRequest(offset + pageSize, pageSize, sort);
  }

  @Override
  public Pageable previousOrFirst() {
    return offset == 0 ? this : new OffsetPageRequest(Math.max(0, offset - pageSize), pageSize, sort);
  }

  @Override
  public Pageable first() {
    return new OffsetPageRequest(0, pageSize, sort);
  }

  @Override
  public Pageable withPage(int pageNumber) {
    if (pageNumber < 0) {
      throw new IllegalArgumentException("Page index must not be negative");
    }
    return new OffsetPageRequest((long) pageNumber * pageSize, pageSize, sort);
  }

  @Override
  public boolean hasPrevious() {
    return offset > 0;
  }
}
