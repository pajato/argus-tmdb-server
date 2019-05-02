# argus-tmdb-client

![TMDB PoweredBy Logo](images/powered-by-tmdb.png)

## Overview

A Kotlin library providing powerful server access to TMDB datasets using paging.

The URL is https://argus.pajato.com:7234/page/<list name>/<start index>/<page size>

where <list name> is one of:
    "collection_ids",
    "keyword_ids",
    "movie_ids",
    "person_ids",
    "production_company_ids",
    "tv_network_ids",
    "tv_series_ids"

Each list contains a, potentially large, number of JSON string recordss identifying a named entity, an identifier that can be used on TMDB to access further information and possibly a few other attributes.  These records are obtained (updated) daily from TMDB datasets. See https://datasets for more details.

The <start index> is a zero based record number.  The <page size> is a chunk size constraining the number of records returned.
