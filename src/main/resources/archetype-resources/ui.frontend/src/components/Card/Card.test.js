import React from 'react';
import { render, screen, cleanup } from '@testing-library/react';

const Card = ({ cardTitle, cardText, imagePath, buttonText, buttonLink }) => (
  <div className="card-showcase" data-testid="card">
    {imagePath && (
      <div className="card-image">
        <img src={imagePath} alt={cardTitle} data-testid="card-image" />
      </div>
    )}
    <div className="card-content">
      {cardTitle && <h3 className="card-title" data-testid="card-title">{cardTitle}</h3>}
      {cardText && <p className="card-text" data-testid="card-text">{cardText}</p>}
      {buttonLink && buttonText && (
        <a href={buttonLink} className="card-button" data-testid="card-button">{buttonText}</a>
      )}
    </div>
  </div>
);

describe('Card Component', () => {
  afterEach(() => { cleanup(); });

  it('renders card with all properties', () => {
    render(<Card cardTitle="Test Title" cardText="Test text" imagePath="/test.jpg" buttonText="Click" buttonLink="/page" />);
    expect(screen.getByTestId('card')).toBeInTheDocument();
    expect(screen.getByTestId('card-title')).toHaveTextContent('Test Title');
  });

  it('renders card without image', () => {
    render(<Card cardTitle="No Image" cardText="Text only" />);
    expect(screen.queryByTestId('card-image')).not.toBeInTheDocument();
  });

  it('renders empty card gracefully', () => {
    render(<Card />);
    expect(screen.getByTestId('card')).toBeInTheDocument();
  });
});
