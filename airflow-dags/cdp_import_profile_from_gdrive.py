
import os
import gdown


def download_files(url_list, output_dir):
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    for url in url_list:
        output = f"{output_dir}/{url.split('/')[-2]}.csv"
        print(f"Downloading {url}...")
        gdown.download(url, output, quiet=False, fuzzy=True)
        print(f"Download completed: {output}")


if __name__ == "__main__":
    # List of public Google Drive URLs to download
    urls = [
        
    ]

    output_directory = "downloads"  # Ensure this directory exists
    download_files(urls, output_directory)
