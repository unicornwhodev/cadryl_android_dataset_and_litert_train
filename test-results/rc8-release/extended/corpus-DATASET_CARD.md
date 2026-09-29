---
language:
- en
license: cc0-1.0
size_categories:
- 1K<n<10K
task_categories:
- image-to-text
- image-to-image
- text-to-image
pretty_name: Stock Images CC0 (public domain)
dataset_info:
  features:
  - name: image
    dtype: image
  - name: tags
    dtype: string
  splits:
  - name: train
    num_bytes: 893124622.125
    num_examples: 3999
  download_size: 888910102
  dataset_size: 893124622.125
configs:
- config_name: default
  data_files:
  - split: train
    path: data/train-*
tags:
- cc0
- public domain
- copyright-free
- stock photos
- images
---

# CC0 Stock Images Dataset

This dataset contains a collection of stock images that are covered by the Creative Commons Zero (CC0) License, meaning they are free for personal and commercial use with no attribution required. It is designed to support a variety of computer vision tasks such as image tagging, categorization, and machine learning model training.

## Disclaimer
While every effort has been made to ensure the reliability and correctness of the data presented, the dataset is provided "as is" without any guarantee. If you find any issues, please report them to the dataset maintainers.

## Dataset Structure

The dataset includes the following features:

- `image`: The raw bytes of the image, which can be read using image processing libraries like PIL or OpenCV.
- `tags`: A string containing comma-separated tags related to the content of the image.

## Size of the Dataset

The size of the dataset is _1000_ images. (To be updated with more soon)

## Use Cases

This dataset can be used for a variety of purposes, including but not limited to:

- Training and evaluating image classification models.
- Developing and testing image tagging algorithms.
- Visual data analysis and machine learning research.
- Creating artwork and design projects.

## License

All images in this dataset are available under the CC0 License. You can copy, modify, distribute, and perform the work, even for commercial purposes, all without asking permission.

## Acknowledgements

This dataset has been compiled from various sources that provide free stock images under the CC0 License. We extend our gratitude to the photographers and creators who have contributed their work to the public domain.

## Accessing the Dataset

This dataset is hosted on the Hugging Face Hub. You can access and download the dataset using the Hugging Face `datasets` library with the following command:

```python
from datasets import load_dataset

dataset = load_dataset('KoalaAI/StockImages-CC0')
```

## Contributing
We welcome contributions to this dataset, whether it's adding more images, improving the tags, or any other improvements you can offer. Please follow the standard procedures for contributing to datasets on the Hugging Face Hub.